#include "native_root.hpp"

#include <cassert>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <string>
#include <vector>

namespace {
using Bytes = std::vector<unsigned char>;

void u32(Bytes& out, std::uint32_t value) {
    for (int shift = 24; shift >= 0; shift -= 8) {
        out.push_back(static_cast<unsigned char>(value >> shift));
    }
}

void i64(Bytes& out, std::int64_t value) {
    const auto raw = static_cast<std::uint64_t>(value);
    for (int shift = 56; shift >= 0; shift -= 8) {
        out.push_back(static_cast<unsigned char>(raw >> shift));
    }
}

void real(Bytes& out, double value) {
    std::uint64_t raw = 0;
    std::memcpy(&raw, &value, sizeof(value));
    i64(out, static_cast<std::int64_t>(raw));
}

void text(Bytes& out, const std::string& value) {
    u32(out, static_cast<std::uint32_t>(value.size()));
    out.insert(out.end(), value.begin(), value.end());
}

void subject(
    Bytes& out, std::int64_t uuid_most, std::int64_t uuid_least,
    const std::string& type, bool player
) {
    out.push_back(1);
    i64(out, uuid_most);
    i64(out, uuid_least);
    text(out, type);
    text(out, player ? "test.Owner" : "test.Zombie");
    text(out, "minecraft:overworld");
    real(out, 1.25);
    real(out, 64.0);
    real(out, -3.5);
    out.push_back(player ? 1 : 0);
}

Bytes snapshot(std::int64_t sequence) {
    Bytes out;
    u32(out, 0x41585233U);
    u32(out, 1U);
    i64(out, 3);
    i64(out, sequence);
    u32(out, 2U);

    out.push_back(0);
    subject(out, 11, 22, "minecraft:player", true);
    i64(out, 3);
    i64(out, sequence);
    out.push_back(1);
    i64(out, 0x7f);
    text(out, "owner");

    out.push_back(1);
    subject(out, 33, 44, "minecraft:zombie", false);
    i64(out, 3);
    i64(out, sequence);
    out.push_back(1);
    out.push_back(2);
    real(out, 0.0);
    text(out, "judged");
    return out;
}

ronova::root::Request request(std::int32_t domain, std::int64_t sequence) {
    ronova::root::Request value;
    value.domain = domain;
    value.generation = 3;
    value.sequence = sequence;
    value.identified = true;
    value.uuid_most = 11;
    value.uuid_least = 22;
    value.registry_type = "minecraft:zombie";
    value.runtime_class = "test.Zombie";
    value.dimension = "minecraft:overworld";
    value.x = 100.0;
    value.y = 70.0;
    value.z = 100.0;
    value.player = false;
    return value;
}

std::uint64_t fingerprint(const Bytes& payload) {
    std::uint64_t value = 0xcbf29ce484222325ULL;
    for (unsigned char current : payload) {
        value ^= current;
        value *= 0x100000001b3ULL;
    }
    return value;
}
} // namespace

int main(int argc, char** argv) {
    using ronova::root::Publication;
    ronova::root::reset();
    const Bytes current = snapshot(19);
    assert(ronova::root::install(current) == Publication::Installed);
    assert(ronova::root::install(current) == Publication::Current);
    assert(ronova::root::export_snapshot() == current);
    assert(ronova::root::capabilities(11, 22) == 0x7f);

    auto status = ronova::root::status();
    assert(status.ready && !status.projection_current);
    assert(status.generation == 3 && status.sequence == 19);
    assert(status.authority_count == 1 && status.judgment_count == 1);
    assert(status.policy_generation == 1 && status.admission_generation == 1);

    auto authority = ronova::root::decide(request(0, 19));
    assert(authority.available && authority.accepted && authority.reason == 1);

    auto judgment_request = request(1, 20);
    judgment_request.facts = 1;
    auto judgment = ronova::root::decide(judgment_request);
    assert(judgment.available && judgment.accepted && judgment.scope == 2);

    auto admission_request = request(2, 19);
    admission_request.facts = 1;
    auto admission = ronova::root::decide(admission_request);
    assert(admission.available && !admission.accepted && admission.reason == 9);

    auto presence = ronova::root::decide(request(3, 19));
    assert(presence.available && !presence.accepted && presence.reason == 13);
    assert(!ronova::root::decide(request(1, 19)).available);

    ronova::root::watchdog_pulse();
    assert(ronova::root::status().drift_detections == 1);
    ronova::root::acknowledge(3, 19, status.fingerprint);
    assert(ronova::root::status().projection_current);

    Bytes conflict = current;
    conflict.back() ^= 1;
    assert(ronova::root::install(conflict) == Publication::Conflict);
    assert(ronova::root::install(snapshot(18)) == Publication::Stale);

    Bytes malformed = current;
    malformed.push_back(0);
    assert(ronova::root::install(malformed) == Publication::Failed);

    const Bytes next = snapshot(20);
    assert(ronova::root::install(next) == Publication::Installed);
    status = ronova::root::status();
    assert(status.policy_generation == 1);
    assert(status.admission_generation == 2);

    ronova::root::reset();
    assert(!ronova::root::status().ready);

    if (argc == 2) {
        std::ifstream input(argv[1], std::ios::binary);
        assert(input.good());
        std::vector<char> raw{
            std::istreambuf_iterator<char>(input), std::istreambuf_iterator<char>()
        };
        Bytes java_fixture(raw.begin(), raw.end());
        assert(ronova::root::install(java_fixture) == Publication::Installed);
        status = ronova::root::status();
        assert(status.fingerprint == fingerprint(java_fixture));
        assert(status.authority_count == 1 && status.judgment_count == 1);
        assert(ronova::root::export_snapshot() == java_fixture);
    }
    std::cout << "native_root_model_test: PASS\n";
}
