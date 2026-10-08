#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace ronova::root {

constexpr std::int32_t CAP_ROOT_STATE = 0x8;
constexpr std::int32_t CAP_ROOT_DECISION = 0x10;
constexpr std::int32_t CAP_ROOT_RECONCILE = 0x20;

enum class Publication : std::int32_t {
    Failed = -1,
    Installed = 0,
    Stale = 1,
    Current = 2,
    Conflict = 3
};

struct Request {
    std::int32_t domain = -1;
    std::int64_t generation = 0;
    std::int64_t sequence = 0;
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
    std::int64_t facts = 0;
};

struct Decision {
    bool available = false;
    bool accepted = false;
    std::int32_t scope = 0;
    std::int32_t reason = 0;
};

struct Status {
    bool ready = false;
    bool projection_current = false;
    std::int64_t generation = 0;
    std::int64_t sequence = 0;
    std::uint64_t fingerprint = 0;
    std::int64_t policy_generation = 0;
    std::int64_t admission_generation = 0;
    std::int64_t reconciliation_clock = 0;
    std::int64_t publications = 0;
    std::int64_t stale_rejections = 0;
    std::int64_t conflicts = 0;
    std::int64_t drift_detections = 0;
    std::int64_t native_decisions = 0;
    std::int64_t authority_count = 0;
    std::int64_t judgment_count = 0;
};

std::int32_t capability_mask();
Publication install(const std::vector<unsigned char>& payload);
std::vector<unsigned char> export_snapshot();
Decision decide(const Request& request);
std::int64_t capabilities(std::int64_t uuid_most, std::int64_t uuid_least);
Status status();
void acknowledge(std::int64_t generation, std::int64_t sequence, std::uint64_t fingerprint);
void watchdog_pulse();
void reset();

} // namespace ronova::root
