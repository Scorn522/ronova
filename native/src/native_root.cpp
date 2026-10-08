#include "native_root.hpp"

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <limits>
#include <mutex>
#include <utility>
#include <vector>

namespace {
constexpr std::uint32_t SNAPSHOT_MAGIC = 0x41585233U;
constexpr std::uint32_t SNAPSHOT_SCHEMA = 1U;
constexpr std::size_t MAX_PAYLOAD = 64U * 1024U * 1024U;
constexpr std::uint32_t MAX_DECISIONS = 131072U;
constexpr std::uint32_t MAX_STRING = 1048576U;
constexpr std::uint64_t FNV_OFFSET = 0xcbf29ce484222325ULL;
constexpr std::uint64_t FNV_PRIME = 0x100000001b3ULL;

struct Epoch {
    std::int64_t generation = 0;
    std::int64_t sequence = 0;
};

struct Subject {
    bool identified = false;
    std::int64_t uuid_most = 0;
    std::int64_t uuid_least = 0;
    std::string registry_type;
    std::string runtime_class;
    std::string dimension;
    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    bool player = false;
};

struct Authority {
    std::int64_t uuid_most = 0;
    std::int64_t uuid_least = 0;
    std::int64_t capability_mask = 0;
};

struct Judgment {
    Subject subject;
    std::int32_t scope = 0;
    double radius = 0.0;
};

struct NativeState {
    bool ready = false;
    Epoch epoch;
    std::uint64_t fingerprint = 0;
    std::uint64_t authority_fingerprint = 0;
    std::uint64_t judgment_fingerprint = 0;
    std::int64_t policy_generation = 0;
    std::int64_t admission_generation = 0;
    std::vector<Authority> authorities;
    std::vector<Judgment> judgments;
    std::vector<unsigned char> payload;
};

std::mutex g_state_mutex;
NativeState g_state;
std::atomic<std::int64_t> g_ack_generation{0};
std::atomic<std::int64_t> g_ack_sequence{0};
std::atomic<std::uint64_t> g_ack_fingerprint{0};
std::atomic<std::int64_t> g_reconciliation_clock{0};
std::atomic<std::int64_t> g_publications{0};
std::atomic<std::int64_t> g_stale_rejections{0};
std::atomic<std::int64_t> g_conflicts{0};
std::atomic<std::int64_t> g_drift_detections{0};
std::atomic<std::int64_t> g_native_decisions{0};
std::atomic<bool> g_drift_latched{false};

class Reader {
public:
    explicit Reader(const std::vector<unsigned char>& bytes) : bytes_(bytes) {}

    bool byte(std::uint8_t& value) {
        if (remaining() < 1) return false;
        value = bytes_[position_++];
        return true;
    }

    bool boolean(bool& value) {
        std::uint8_t raw = 0;
        if (!byte(raw) || raw > 1) return false;
        value = raw != 0;
        return true;
    }

    bool u32(std::uint32_t& value) {
        if (remaining() < 4) return false;
        value = (static_cast<std::uint32_t>(bytes_[position_]) << 24U)
            | (static_cast<std::uint32_t>(bytes_[position_ + 1]) << 16U)
            | (static_cast<std::uint32_t>(bytes_[position_ + 2]) << 8U)
            | static_cast<std::uint32_t>(bytes_[position_ + 3]);
        position_ += 4;
        return true;
    }

    bool i64(std::int64_t& value) {
        std::uint64_t raw = 0;
        if (!u64(raw)) return false;
        std::memcpy(&value, &raw, sizeof(value));
        return true;
    }

    bool real(double& value) {
        std::uint64_t raw = 0;
        if (!u64(raw)) return false;
        std::memcpy(&value, &raw, sizeof(value));
        return std::isfinite(value);
    }

    bool text(std::string& value) {
        std::uint32_t length = 0;
        if (!u32(length) || length > MAX_STRING || remaining() < length) return false;
        value.assign(reinterpret_cast<const char*>(bytes_.data() + position_), length);
        position_ += length;
        return true;
    }

    std::size_t position() const { return position_; }
    std::size_t remaining() const { return bytes_.size() - position_; }

private:
    bool u64(std::uint64_t& value) {
        if (remaining() < 8) return false;
        value = 0;
        for (int index = 0; index < 8; ++index) {
            value = (value << 8U) | bytes_[position_ + index];
        }
        position_ += 8;
        return true;
    }

    const std::vector<unsigned char>& bytes_;
    std::size_t position_ = 0;
};

std::uint64_t fingerprint(const unsigned char* data, std::size_t length) {
    std::uint64_t value = FNV_OFFSET;
    for (std::size_t index = 0; index < length; ++index) {
        value ^= data[index];
        value *= FNV_PRIME;
    }
    return value;
}

void extend_fingerprint(
    std::uint64_t& value, const std::vector<unsigned char>& payload,
    std::size_t start, std::size_t end
) {
    for (std::size_t offset = start; offset < end; ++offset) {
        value ^= payload[offset];
        value *= FNV_PRIME;
    }
}

void extend_i64(std::uint64_t& value, std::int64_t signed_value) {
    const auto raw = static_cast<std::uint64_t>(signed_value);
    for (int shift = 56; shift >= 0; shift -= 8) {
        value ^= (raw >> shift) & 0xffU;
        value *= FNV_PRIME;
    }
}

bool read_subject(Reader& reader, Subject& subject) {
    return reader.boolean(subject.identified)
        && reader.i64(subject.uuid_most)
        && reader.i64(subject.uuid_least)
        && reader.text(subject.registry_type)
        && reader.text(subject.runtime_class)
        && reader.text(subject.dimension)
        && reader.real(subject.x)
        && reader.real(subject.y)
        && reader.real(subject.z)
        && reader.boolean(subject.player);
}

bool valid_epoch(const Epoch& epoch) {
    return epoch.generation >= 0 && epoch.sequence >= 0;
}

int compare_epoch(const Epoch& left, const Epoch& right) {
    if (left.generation != right.generation) {
        return left.generation < right.generation ? -1 : 1;
    }
    if (left.sequence == right.sequence) return 0;
    return left.sequence < right.sequence ? -1 : 1;
}

bool next_epoch(const Epoch& current, Epoch& next) {
    next = current;
    if (next.sequence == std::numeric_limits<std::int64_t>::max()) {
        if (next.generation == std::numeric_limits<std::int64_t>::max()) return false;
        ++next.generation;
        next.sequence = 0;
    } else {
        ++next.sequence;
    }
    return true;
}

bool parse_snapshot(const std::vector<unsigned char>& payload, NativeState& state) {
    if (payload.empty() || payload.size() > MAX_PAYLOAD) return false;
    Reader reader(payload);
    std::uint32_t magic = 0;
    std::uint32_t schema = 0;
    std::uint32_t count = 0;
    if (!reader.u32(magic) || magic != SNAPSHOT_MAGIC
        || !reader.u32(schema) || schema != SNAPSHOT_SCHEMA
        || !reader.i64(state.epoch.generation)
        || !reader.i64(state.epoch.sequence)
        || !valid_epoch(state.epoch)
        || !reader.u32(count) || count > MAX_DECISIONS) {
        return false;
    }

    std::uint64_t authority_hash = FNV_OFFSET;
    std::uint64_t judgment_hash = FNV_OFFSET;
    for (std::uint32_t index = 0; index < count; ++index) {
        const std::size_t decision_start = reader.position();
        std::uint8_t domain = 0;
        Subject subject;
        Epoch decision_epoch;
        if (!reader.byte(domain) || domain > 3 || !read_subject(reader, subject)
            || !reader.i64(decision_epoch.generation)
            || !reader.i64(decision_epoch.sequence)
            || !valid_epoch(decision_epoch)
            || compare_epoch(decision_epoch, state.epoch) > 0) {
            return false;
        }

        bool accepted = false;
        if (!reader.boolean(accepted)) return false;
        if (domain == 0) {
            std::int64_t capabilities = 0;
            if (!reader.i64(capabilities)) return false;
            if (accepted && subject.identified) {
                state.authorities.push_back(Authority{
                    subject.uuid_most, subject.uuid_least, capabilities
                });
                extend_i64(authority_hash, subject.uuid_most);
                extend_i64(authority_hash, subject.uuid_least);
                extend_i64(authority_hash, capabilities);
            }
        } else if (domain == 1) {
            std::uint8_t scope = 0;
            double radius = 0.0;
            if (!reader.byte(scope) || scope > 2 || !reader.real(radius) || radius < 0.0) {
                return false;
            }
            if (accepted) {
                state.judgments.push_back(Judgment{std::move(subject), scope, radius});
            }
        }

        std::string reason;
        if (!reader.text(reason)) return false;
        const std::size_t decision_end = reader.position();
        if (domain == 1) {
            extend_fingerprint(judgment_hash, payload, decision_start, decision_end);
        }
    }
    if (reader.remaining() != 0) return false;
    state.ready = true;
    state.payload = payload;
    state.fingerprint = fingerprint(payload.data(), payload.size());
    state.authority_fingerprint = authority_hash;
    state.judgment_fingerprint = judgment_hash;
    return true;
}

bool uuid_equal(const Subject& subject, const ronova::root::Request& request) {
    return request.identified && subject.identified
        && subject.uuid_most == request.uuid_most
        && subject.uuid_least == request.uuid_least;
}

bool type_equal(const Judgment& judgment, const ronova::root::Request& request) {
    return judgment.subject.registry_type == request.registry_type
        || (judgment.subject.runtime_class != "unknown"
            && judgment.subject.runtime_class == request.runtime_class);
}

bool matches(const NativeState& state, const ronova::root::Request& request) {
    for (const Judgment& judgment : state.judgments) {
        if (judgment.scope == 0) {
            if (uuid_equal(judgment.subject, request)) return true;
            continue;
        }
        if (!type_equal(judgment, request)) continue;
        if (judgment.scope == 2) return true;
        if (judgment.subject.dimension != request.dimension) continue;
        const double dx = request.x - judgment.subject.x;
        const double dy = request.y - judgment.subject.y;
        const double dz = request.z - judgment.subject.z;
        if (dx * dx + dy * dy + dz * dz <= judgment.radius * judgment.radius) return true;
    }
    return false;
}

ronova::root::Decision result(bool accepted, std::int32_t scope, std::int32_t reason) {
    return ronova::root::Decision{true, accepted, scope, reason};
}
} // namespace

namespace ronova::root {
std::int32_t capability_mask() {
    return CAP_ROOT_STATE | CAP_ROOT_DECISION | CAP_ROOT_RECONCILE;
}

Publication install(const std::vector<unsigned char>& payload) {
    NativeState next;
    if (!parse_snapshot(payload, next)) return Publication::Failed;

    std::lock_guard<std::mutex> guard(g_state_mutex);
    if (g_state.ready) {
        const int order = compare_epoch(next.epoch, g_state.epoch);
        if (order < 0) {
            g_stale_rejections.fetch_add(1, std::memory_order_relaxed);
            return Publication::Stale;
        }
        if (order == 0) {
            if (next.fingerprint == g_state.fingerprint) return Publication::Current;
            g_conflicts.fetch_add(1, std::memory_order_relaxed);
            return Publication::Conflict;
        }
        next.policy_generation = next.authority_fingerprint == g_state.authority_fingerprint
            ? g_state.policy_generation : g_state.policy_generation + 1;
        next.admission_generation = next.judgment_fingerprint == g_state.judgment_fingerprint
            ? g_state.admission_generation : g_state.admission_generation + 1;
    } else {
        next.policy_generation = 1;
        next.admission_generation = 1;
    }
    g_state = std::move(next);
    g_publications.fetch_add(1, std::memory_order_relaxed);
    g_drift_latched.store(false, std::memory_order_relaxed);
    return Publication::Installed;
}

std::vector<unsigned char> export_snapshot() {
    std::lock_guard<std::mutex> guard(g_state_mutex);
    return g_state.payload;
}

Decision decide(const Request& request) {
    if (request.domain < 0 || request.domain > 3
        || request.generation < 0 || request.sequence < 0
        || !std::isfinite(request.x) || !std::isfinite(request.y)
        || !std::isfinite(request.z)) {
        return {};
    }

    std::lock_guard<std::mutex> guard(g_state_mutex);
    if (!g_state.ready) return {};
    const Epoch requested{request.generation, request.sequence};
    Epoch next;
    const bool current = compare_epoch(requested, g_state.epoch) == 0;
    const bool proposed = next_epoch(g_state.epoch, next)
        && compare_epoch(requested, next) == 0;
    if ((request.domain == 1 && !proposed)
        || (request.domain != 1 && !current && !(request.domain == 0 && proposed))) {
        return {};
    }

    g_native_decisions.fetch_add(1, std::memory_order_relaxed);
    if (request.domain == 0) {
        if (proposed) {
            const bool owner = (request.facts & 1) != 0;
            return result(owner, 0, owner ? 15 : 16);
        }
        const bool owner = std::any_of(
            g_state.authorities.begin(), g_state.authorities.end(),
            [&](const Authority& authority) {
                return request.identified && authority.uuid_most == request.uuid_most
                    && authority.uuid_least == request.uuid_least;
            }
        );
        return result(owner, 0, owner ? 1 : 2);
    }
    if (request.domain == 1) {
        const bool actor = (request.facts & 1) != 0;
        const bool terminal = (request.facts & 2) != 0;
        const std::int32_t scope = request.player ? 0 : 2;
        return !actor ? result(false, scope, 3)
            : terminal ? result(false, scope, 4) : result(true, scope, 5);
    }
    const bool quarantined = matches(g_state, request);
    if (request.domain == 2) {
        const bool combat = (request.facts & 1) != 0;
        const bool protected_subject = (request.facts & 2) != 0;
        const bool terminal = (request.facts & 4) != 0;
        const bool passed_final = (request.facts & 16) != 0;
        const bool final_boundary = (request.facts & 32) != 0;
        if (!combat) return result(true, 0, 6);
        if (protected_subject) return result(true, 0, 7);
        if (terminal) return result(false, 0, 8);
        if (quarantined) return result(false, 0, 9);
        if (final_boundary && passed_final) return result(true, 0, 10);
        return result(true, 0, final_boundary ? 11 : 12);
    }
    return result(!quarantined, 0, quarantined ? 13 : 14);
}

std::int64_t capabilities(std::int64_t uuid_most, std::int64_t uuid_least) {
    std::lock_guard<std::mutex> guard(g_state_mutex);
    for (const Authority& authority : g_state.authorities) {
        if (authority.uuid_most == uuid_most && authority.uuid_least == uuid_least) {
            return authority.capability_mask;
        }
    }
    return 0;
}

Status status() {
    Status value;
    {
        std::lock_guard<std::mutex> guard(g_state_mutex);
        value.ready = g_state.ready;
        value.projection_current = g_state.ready
            && g_ack_generation.load(std::memory_order_relaxed) == g_state.epoch.generation
            && g_ack_sequence.load(std::memory_order_relaxed) == g_state.epoch.sequence
            && g_ack_fingerprint.load(std::memory_order_relaxed) == g_state.fingerprint;
        value.generation = g_state.epoch.generation;
        value.sequence = g_state.epoch.sequence;
        value.fingerprint = g_state.fingerprint;
        value.policy_generation = g_state.policy_generation;
        value.admission_generation = g_state.admission_generation;
        value.authority_count = static_cast<std::int64_t>(g_state.authorities.size());
        value.judgment_count = static_cast<std::int64_t>(g_state.judgments.size());
    }
    value.reconciliation_clock = g_reconciliation_clock.load(std::memory_order_relaxed);
    value.publications = g_publications.load(std::memory_order_relaxed);
    value.stale_rejections = g_stale_rejections.load(std::memory_order_relaxed);
    value.conflicts = g_conflicts.load(std::memory_order_relaxed);
    value.drift_detections = g_drift_detections.load(std::memory_order_relaxed);
    value.native_decisions = g_native_decisions.load(std::memory_order_relaxed);
    return value;
}

void acknowledge(
    std::int64_t generation, std::int64_t sequence, std::uint64_t fingerprint_value
) {
    g_ack_generation.store(generation, std::memory_order_relaxed);
    g_ack_sequence.store(sequence, std::memory_order_relaxed);
    g_ack_fingerprint.store(fingerprint_value, std::memory_order_relaxed);
    g_drift_latched.store(false, std::memory_order_relaxed);
}

void watchdog_pulse() {
    g_reconciliation_clock.fetch_add(1, std::memory_order_relaxed);
    bool mismatch = false;
    {
        std::lock_guard<std::mutex> guard(g_state_mutex);
        mismatch = g_state.ready && (
            g_ack_generation.load(std::memory_order_relaxed) != g_state.epoch.generation
            || g_ack_sequence.load(std::memory_order_relaxed) != g_state.epoch.sequence
            || g_ack_fingerprint.load(std::memory_order_relaxed) != g_state.fingerprint
        );
    }
    if (mismatch) {
        if (!g_drift_latched.exchange(true, std::memory_order_relaxed)) {
            g_drift_detections.fetch_add(1, std::memory_order_relaxed);
        }
    } else {
        g_drift_latched.store(false, std::memory_order_relaxed);
    }
}

void reset() {
    {
        std::lock_guard<std::mutex> guard(g_state_mutex);
        g_state = NativeState{};
    }
    g_ack_generation.store(0, std::memory_order_relaxed);
    g_ack_sequence.store(0, std::memory_order_relaxed);
    g_ack_fingerprint.store(0, std::memory_order_relaxed);
    g_publications.store(0, std::memory_order_relaxed);
    g_stale_rejections.store(0, std::memory_order_relaxed);
    g_conflicts.store(0, std::memory_order_relaxed);
    g_drift_detections.store(0, std::memory_order_relaxed);
    g_native_decisions.store(0, std::memory_order_relaxed);
    g_drift_latched.store(false, std::memory_order_relaxed);
}
} // namespace ronova::root
