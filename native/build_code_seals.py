"""Embed build-time registered class images in Native, then finalize the exact production JAR."""
from pathlib import Path
import hashlib,json,os,subprocess,sys,zipfile
from code_seal_inventory import select_registered_images
from normalize_pe_timestamp import normalized_timestamp

ROOT=Path(__file__).resolve().parents[1]
JAR=Path(sys.argv[1]).resolve()
BUILD=ROOT/'build/native-code-seals'
BUILD.mkdir(parents=True,exist_ok=True)
PREFIXES=[
 'dev/ronova/coremod/persistence/ShadowDurableRootStore',
 'dev/ronova/coremod/persistence/LocalDominionStore',
 'dev/ronova/coremod/persistence/DurableRootFileBackend',
 'dev/ronova/coremod/persistence/RootCommandJournal',
 'dev/ronova/coremod/persistence/EffectJournal',
 'dev/ronova/coremod/persistence/ObligationCheckpointCodec',
 'dev/ronova/coremod/obligation/ObligationCheckpoint',
 'dev/ronova/coremod/obligation/ObligationRegistry',
 'dev/ronova/coremod/obligation/ObligationCoordinator',
 'dev/ronova/coremod/obligation/CandidateRestrictions',
 'dev/ronova/coremod/obligation/RestorationGapBuffer',
 'dev/ronova/coremod/obligation/OverflowCheckpoint',
 'dev/ronova/coremod/root/api/EffectRecord',
 'dev/ronova/coremod/runtime/RootCommandBus',
 'dev/ronova/coremod/runtime/DominionServerRuntime',
 'dev/ronova/coremod/runtime/IngressWorkRegistry',
 'dev/ronova/coremod/runtime/DeferredAuthorityTask',
 'dev/ronova/coremod/runtime/PlanExecutionLease',
 'dev/ronova/coremod/runtime/OperationPlanRunner',
 'dev/ronova/coremod/runtime/CompletionTracker',
 'dev/ronova/coremod/contract/ClassLineageResolver',
 'dev/ronova/coremod/engine/TerminalCompletionMonitor',
 'dev/ronova/coremod/engine/TerminalIndexRepair',
 'dev/ronova/coremod/terminal/DurableTerminalWatchSet',
 'dev/ronova/coremod/engine/TerminalPlanExecutor',
 'dev/ronova/coremod/engine/TerminalStateSink',
 'dev/ronova/coremod/engine/TerminalStateFeedback',
 'dev/ronova/coremod/runtime/StateFeedbackSchedule',
 'dev/ronova/coremod/runtime/RuntimeCoordinator',
 'dev/ronova/coremod/capability/recovery/RegisteredOwnerVitals',
 'dev/ronova/coremod/capability/recovery/RegisteredOwnerIdentity',
 'dev/ronova/coremod/engine/RegisteredIntIndexSlots',
 'dev/ronova/coremod/capability/AuthorityRuntimeState',
 'dev/ronova/coremod/capability/recovery/VitalsRecoveryModule',
 'dev/ronova/coremod/engine/TerminalLifecycle',
 'dev/ronova/coremod/network/ServerActionService',
 'dev/ronova/coremod/network/StrengthControlService',
 'dev/ronova/coremod/network/StrengthStatusDetail',
 'dev/ronova/coremod/event/DominionEvents',
 'dev/ronova/coremod/agent/NativeTransformBridge',
 'dev/ronova/coremod/agent/BootstrapTaskHooks',
 'dev/ronova/coremod/agent/JdkTaskBoundaryContract',
 'dev/ronova/coremod/agent/SemanticGateVerifier',
 'dev/ronova/coremod/agent/RawAccessorContract',
 'dev/ronova/coremod/agent/EntityDataAccessorContract',
 'dev/ronova/coremod/agent/NbtCopyContract',
 'dev/ronova/coremod/agent/NbtFactoryContract',
 'dev/ronova/coremod/observation/CopyProvenanceIndex',
 'dev/ronova/coremod/observation/AtomicSnapshotReference',
 'dev/ronova/coremod/observation/AtomicSnapshotRecord',
 'dev/ronova/coremod/observation/RecordMutationTask',
 'dev/ronova/coremod/observation/DeferredReceiptBuffer',
 'dev/ronova/coremod/observation/RetiredWorldAdmission',
 'dev/ronova/coremod/bootstrap/ServerRuntimeRegistry',
 'dev/ronova/coremod/mixin/DimensionDataStorageRecoveryMixin',
 'dev/ronova/coremod/mixin/SavedDataRecoverySaveMixin',
 'dev/ronova/coremod/observation/SavedDataSaveTrace',
 'dev/ronova/coremod/observation/SavedDataEncodedWrite',
 'dev/ronova/coremod/observation/NbtWriteReferences',
 'dev/ronova/coremod/observation/StorageAdapterFingerprint',
 'dev/ronova/coremod/observation/RecoveryFileReadback',
 'dev/ronova/coremod/observation/RecoveryStorageReadbackQuery',
 'dev/ronova/coremod/observation/NbtStoragePreservation',
 'dev/ronova/coremod/observation/NbtEncodingPaths',
 'dev/ronova/coremod/observation/NbtStorageImage',
 'dev/ronova/coremod/observation/StorageImageTask',
 'dev/ronova/coremod/observation/StorageTransactionTask',
 'dev/ronova/coremod/observation/SavedDataSaveTracked',
 'dev/ronova/coremod/observation/StorageMutationSequence',
 'dev/ronova/coremod/observation/StorageMutationTracked',
 'dev/ronova/coremod/observation/StorageBindingObservation',
 'dev/ronova/coremod/observation/LoadedSnapshotRecordBinding',
 'dev/ronova/coremod/observation/RecoveryLocationResolver',
 'dev/ronova/coremod/observation/RecordRetrySchedule',
 'dev/ronova/coremod/observation/StaticSnapshotLocator',
 'dev/ronova/coremod/observation/InstanceFieldCursor',
 'dev/ronova/coremod/observation/ReflectionFieldLayout',
 'dev/ronova/coremod/observation/DiscoveryCursor',
 'dev/ronova/coremod/observation/RecoveryObservationScope',
 'dev/ronova/coremod/observation/RecoveryTaskObserver',
 'dev/ronova/coremod/observation/RecoveryTaskDisposition',
 'dev/ronova/coremod/observation/TaskExecutionGraph',
 'dev/ronova/coremod/observation/TaskEventRun',
 'dev/ronova/coremod/observation/RecoveryBoundaryProof',
 'dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime',
 'dev/ronova/coremod/agent/BootstrapHooks',
 'dev/ronova/coremod/integrity/EnforcementRuntime',
 'dev/ronova/coremod/integrity/EnforcementProof',
 'dev/ronova/coremod/integrity/EnforcementProofSource',
 'dev/ronova/coremod/integrity/CapabilityChallengeProof',
 'dev/ronova/coremod/integrity/CodeIntegrityProof',
 'dev/ronova/coremod/nativebridge/NativeIntegrityBridge',
 'dev/ronova/coremod/observation/RecoverySourceTracker',
 'dev/ronova/coremod/policy/RecoveryChainPolicy',
 'dev/ronova/coremod/policy/TerminalActionPolicy',
 'dev/ronova/coremod/policy/RuntimeCapabilityProjection',
 'dev/ronova/coremod/nativebridge/NativeBridge',
 'dev/ronova/coremod/nativebridge/RuntimeArtifactSource',
 'dev/ronova/coremod/integrity/CodeSealProbe',
 'dev/ronova/coremod/integrity/RawAccessorDispatch',
 'dev/ronova/coremod/root/api/CommittedJudgmentGate',
 'dev/ronova/coremod/root/api/RootExecutionFence',
 'dev/ronova/coremod/root/api/WriteSetLease',
 'dev/ronova/coremod/root/api/WriteSetReceipt',
 'dev/ronova/coremod/root/api/OperationReport',
 'dev/ronova/coremod/root/api/SubjectQualification',
 'dev/ronova/coremod/root/api/QualificationPreconditions',
 'dev/ronova/coremod/root/api/RecoverySourcePlan',
 'dev/ronova/coremod/root/api/RecoverySnapshotPlan',
 'dev/ronova/coremod/root/api/RecoveryRecordPlan',
 'dev/ronova/coremod/root/api/RecoveryMutationIntent',
 'dev/ronova/coremod/root/api/RecoveryTaskPlan',
 'dev/ronova/coremod/root/api/RecoveryStoragePlan',
 'dev/ronova/coremod/root/api/RecoveryWorkDescriptor',
 'dev/ronova/coremod/root/api/RecoveryStorageGeneration',
 'dev/ronova/coremod/root/api/RecoveryWorkResult',
 'dev/ronova/coremod/root/api/RecoverySourceEvidence',
 'dev/ronova/coremod/root/api/RecoverySourceLocation',
]
with zipfile.ZipFile(JAR) as source:
 images=select_registered_images(source,PREFIXES)
manifest={name:hashlib.sha256(data).hexdigest() for name,data in images.items()}
build_id=hashlib.sha256(json.dumps(manifest,sort_keys=True,separators=(',',':')).encode()).hexdigest()
header=['#pragma once','#define RONOVA_CODE_SEALS 1',f'static const char* CODE_SEAL_BUILD_ID = "{build_id}";',
 'struct RegisteredCodeImage { const char* name; const unsigned char* data; jint length; std::atomic<bool> proposed; std::atomic<bool> verified; std::atomic<bool> loaded; };']
for index,data in enumerate(images.values()):
 header.append(f'static const unsigned char code_image_{index}[] = {{'+','.join(str(b) for b in data)+'};')
header.append('static RegisteredCodeImage CODE_IMAGES[] = {')
for index,(name,data) in enumerate(images.items()):header.append(f'{{"{name}",code_image_{index},{len(data)},false,false,false}},')
header.append('};')
(BUILD/'registered_code_images.hpp').write_text('\n'.join(header)+'\n',encoding='utf-8')
(BUILD/'manifest.json').write_text(json.dumps({'build_id':build_id,'classes':manifest},indent=2)+'\n',encoding='utf-8')
tools=ROOT.parent/'.build-tools'
zig=Path(os.environ.get('RONOVA_ZIG',str(tools/'zig-windows-x86_64-0.13.0/zig.exe')))
include=Path(os.environ.get('RONOVA_JDK_INCLUDE',str(tools/'jdk17-headers')))
if not zig.is_file() or not (include/'jvmti.h').is_file():raise RuntimeError('Code-seal build requires RONOVA_ZIG and RONOVA_JDK_INCLUDE')
env={**os.environ,'ZIG_GLOBAL_CACHE_DIR':str(tools/'zig-cache')}
dll=BUILD/'ronova.dll'
subprocess.run([str(zig),'c++','-target','x86_64-windows-gnu','-shared','-O2','-s','-std=c++17',
 '-I'+str(include),'-I'+str(include/'win32'),'-I'+str(BUILD),
 str(ROOT/'native/src/ronova_native.cpp'),'-lbcrypt','-o',str(dll)],check=True,env=env)
# Zig 0.13 COFF timestamps are wall-clock dependent; normalize before embedding/signing.
dll.write_bytes(normalized_timestamp(dll.read_bytes()))

# reobfJar may reconstruct the production archive without retaining a nested task-hook JAR.
# taskHookJar is already a dependency of jar, so make the exact finalizer authoritative for this
# runtime-critical bootstrap resource and fail the build rather than shipping a silent bridge gap.
task_hook=ROOT/'build/agent-bootstrap/task-hook-support.jar'
if not task_hook.is_file():raise RuntimeError('Missing built task-hook-support.jar')
task_hook_bytes=task_hook.read_bytes()
with zipfile.ZipFile(task_hook) as hooks:
 if 'dev/ronova/coremod/agent/BootstrapTaskHooks.class' not in hooks.namelist():
  raise RuntimeError('task-hook-support.jar is missing BootstrapTaskHooks.class')

temporary=JAR.with_suffix('.sealed.tmp')
replaced={'native/windows-x86_64/ronova.dll','META-INF/ronova/code-seal-id.txt','META-INF/ronova/code-seal-manifest.json',
          'META-INF/ronova/task-hook-support.jar'}
with zipfile.ZipFile(JAR) as source,zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as target:
 for entry in source.infolist():
  if entry.filename not in replaced:target.writestr(entry,source.read(entry.filename))
 for name,data in [('native/windows-x86_64/ronova.dll',dll.read_bytes()),('META-INF/ronova/code-seal-id.txt',build_id.encode()),
                   ('META-INF/ronova/code-seal-manifest.json',(BUILD/'manifest.json').read_bytes()),
                   ('META-INF/ronova/task-hook-support.jar',task_hook_bytes)]:
  entry=zipfile.ZipInfo(name,(1980,1,1,0,0,0));entry.compress_type=zipfile.ZIP_DEFLATED;target.writestr(entry,data)
os.replace(temporary,JAR)
print(f'Code seals: {len(images)} registered classes; build_id={build_id}',flush=True)
