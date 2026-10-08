# R66 · 八方向已有基线补遗：独立源码依据
源码ZIP SHA-256：`1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。日期：2026-09-15。只读范围和逐文件哈希随[融合清单](../plans/restructure/enhancement-addendum-merge.json)保存。
[原始补遗](R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md) · [当前接线](../plans/R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) · [处理回执](../plans/RESTRUCTURE-REVIEW.md#review-fix11-baseline)
<a id="findings"></a>
## 本轮独立源码补读与采用边界

来源正文已经完整读取；以下为对所给 **r66 源包**的静态核对与设计处理，不回改来源，不证明工作区后续代码或运行行为。没有使用外部研究来替代补遗。作者“约28类免责声明”的数字仍是作者人工下界，本轮不认证这个计数，也不把机械枚举等同于逐方法语义审完。

| 项目 | 源码实际支持 | 本轮采用的范围 |
|---|---|---|
| 观察不授权 | `CopyProvenanceIndex`、`StorageMutationSequence`、读回类等明确保留非授权契约；执行消费原Root计划与租约 | 观察可以导致失证/拒绝；不能自行发行许可、补全Truth或完成事实。S3是结果评估，A4是可用性/撤销接线，不把两者统称为新增授权者。补遗的“三条硬约束”作为当前分层纪律采用，不另升为L0或永久冻结条款。 |
| S2写入接缝 | `WriteSinkId/Evidence/Extension`和版本codec存在；Evidence带operationId、sink、epoch、policy、outcome、hash | 接入既有具名接缝，不另造平行批准者。字段合法/哈希非空不是可信发行；`begins in 7.17`是注释中的阶段意图，不证明生产写集已接通。 |
| S2实际写入 | `NbtWriteReferences`记录受限实际NBT写遍历，弱引用与路径有完整性标志；不是全部内存写审计 | 复用支持的NBT出口，不宣称实体/追踪/原生等表面的actual-write一侧已完整。`complete=false`、弱引用已失效、未插桩均保留未知，不能当空写集通过。 |
| S2已有比对 | `RecoverySourceTracker.writtenPositionsMatch`核完整性，按实际对象/复制来源筛路径，再与encoding.positions集合相等比较 | 不重写已有保存位置/来源比对；它不等于声明式allowedSurfaces/Objects/Operations及最终写权限，通用合同仍待接。 |
| S3完成接缝 | `CompletionContractId/Extension/Evaluation`存在；Id明指non-CLEARED；Evaluation仍是证据 | 复用指定完成合同与原Root事实，不把构造出`completionSatisfied=true`当成认证；不替代CLEARED终结语义，不因注释认定生产finality已经完成。 |
| S3回执 | `DeferredReceiptBuffer`先留票，再执行；`RecoverySourceTracker`在变更前reserve，最后complete，flush只发布原结果 | 复用“只重试回执、不重做变更”和累积未知纪律。类是observation包内可见，跨域推广需要适配/受控提取，不简单公开类后赋权。 |
| S4连续性 | `TaskEventRun`限制同任务/父/调度器/proof与变化计数，消费者已有失证 | fix10已经提及该压缩器；本轮补精确契约与读前入口，不重开一套序列方案。跨源代际及重建仍是增量。 |
| A1索引 | `CopyProvenanceIndex`已有对象身份图索引、`bySource`反向表、冲突/容量损失回调和source退役；tracker把loss接到targetEpoch的discoveryGap | 不再按“没有反向索引、没有局部失证”估工。它仍不是grant→installed→proof→capability→executor通用依赖图，不能将所有边强塞入NBT复制模型；新索引也不授予Root权限。 |
| S1载体 | `NativeTransformBridge`确实委派`RonovaTransformer`；后者分域调合同/变换与校验，probe是代码金丝雀 | 先读真实transformer与原域合同，再做隔离挑战适配；553行是该输入文件规模，不是挑战覆盖率或已支持全部场景。 |
| A2 | build identity与capabilityMask存在；`enableCodeSeals0(id)`调用返回值在该Java位置没有消费 | 将该结果接入协商/诊断；不外推成所有封印证明都不存在。ABI/版本化feature协议仍是增量；只为实际跨边界结构协商尺寸。 |
| A3 | `ShadowVerification`十态且`accepted()`与`matches()`不同 | 模板/判定纪律复用，不将持久根专用的realm/hash/receipt词汇强套成所有新能力唯一结果enum；未执行影子仍不能报误判率。 |
| A4宿主 | `CapabilityRuntime`登记六个修复模块，以`contract.Capability`过滤，唯一维护owner且捕获失败计数 | 这是所覆盖服务端修复模块的现成宿主，不是全部54个`policy.CapabilityKey`和Native回调的统一执行器；先建立明确映射/作用范围，其他域沿已有owner适配，不新开tick。 |
| A4重试 | 包内`engine.spawn.BoundedRetryQueue`已有FIFO、false结果的有限重试/死信；`process`抛RuntimeException/Error时原entry回队并重抛 | 不把它当“任意异常均已有限重试”的全局熔断器。复用前定义返回false、抛异常、未知效果的分类和版本闸；不得把副作用不明的mutation自动重新执行。 |
| A4成功信号 | `CapabilityContext.attempt`可在内部捕获异常并记数，外层module.repair可能正常返回 | 正常返回不能独立作为熔断恢复成功；接实际范围的结果/失败证据。既有状态/表面成熟度仍不因补文档而升档。 |

下列六个具名接缝标识的限定词法扫描只在`src/main/java/**/*.java`执行；记录全部原行，包括定义与泛型引用。该范围所见引用集中在API/接口，不冒充对反射、生成代码、所有扩展注册方式的穷举证明。

<a id="inventory"></a>
## 子系统枚举范围

文件数为机械枚举；实际精读范围以本页代码摘录为准，不据关键词零命中推出功能不存在。

| 相对目录 | 递归 | Java文件数 |
|---|---|---:|
| `src/main/java/dev/ronova/coremod/observation` | 否 | 41 |
| `src/main/java/dev/ronova/coremod/capability` | 是 | 13 |
| `src/main/java/dev/ronova/coremod/root/api` | 否 | 40 |
| `src/main/java/dev/ronova/coremod/integrity` | 是 | 6 |
| `src/main/java/dev/ronova/coremod/surface` | 否 | 4 |
| `src/main/java/dev/ronova/coremod/budget` | 是 | 10 |
| `src/main/java/dev/ronova/coremod/runtime` | 是 | 10 |

<a id="seam-references"></a>
## 具名接缝的生产源码词法引用

```text
src/main/java/dev/ronova/coremod/root/api/CompletionContractExtension.java:6: public interface CompletionContractExtension
src/main/java/dev/ronova/coremod/root/api/CompletionContractExtension.java:7:     extends VersionedExtensionCodec<CompletionEvaluation> {}
src/main/java/dev/ronova/coremod/root/api/CompletionContractId.java:8: public record CompletionContractId(String canonical) {
src/main/java/dev/ronova/coremod/root/api/CompletionContractId.java:12:     public CompletionContractId {
src/main/java/dev/ronova/coremod/root/api/CompletionEvaluation.java:8: public record CompletionEvaluation(
src/main/java/dev/ronova/coremod/root/api/CompletionEvaluation.java:10:     CompletionContractId contract,
src/main/java/dev/ronova/coremod/root/api/CompletionEvaluation.java:16:     public CompletionEvaluation {
src/main/java/dev/ronova/coremod/root/api/WriteSinkEvidence.java:9: public record WriteSinkEvidence(
src/main/java/dev/ronova/coremod/root/api/WriteSinkEvidence.java:11:     WriteSinkId sink,
src/main/java/dev/ronova/coremod/root/api/WriteSinkEvidence.java:17:     public WriteSinkEvidence {
src/main/java/dev/ronova/coremod/root/api/WriteSinkExtension.java:6: public interface WriteSinkExtension extends VersionedExtensionCodec<WriteSinkEvidence> {}
src/main/java/dev/ronova/coremod/root/api/WriteSinkId.java:8: public record WriteSinkId(String canonical) {
src/main/java/dev/ronova/coremod/root/api/WriteSinkId.java:12:     public WriteSinkId {
```

## 原源码摘录

行号以收到的r66源包为准；代码保持原文本，仅展示统一换行。摘录不是本轮改动，也不是运行证据。

<a id="sink-id"></a>
### `src/main/java/dev/ronova/coremod/root/api/WriteSinkId.java` · 1–18

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Locale;
   4  import java.util.Objects;
   5  import java.util.regex.Pattern;
   6  
   7  /** Stable identifier for an Authority-bearing final write boundary. */
   8  public record WriteSinkId(String canonical) {
   9      private static final Pattern VALUE =
  10          Pattern.compile("[a-z0-9][a-z0-9_.:/-]{0,191}");
  11  
  12      public WriteSinkId {
  13          canonical = Objects.toString(canonical, "").trim().toLowerCase(Locale.ROOT);
  14          if (!VALUE.matcher(canonical).matches()) {
  15              throw new IllegalArgumentException("invalid write sink id: " + canonical);
  16          }
  17      }
  18  }
```

<a id="sink-evidence"></a>
### `src/main/java/dev/ronova/coremod/root/api/WriteSinkEvidence.java` · 1–36

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.policy.PolicyRevision;
   4  import java.util.Locale;
   5  import java.util.Objects;
   6  import java.util.UUID;
   7  
   8  /** Bounded evidence emitted by a registered final write gate. */
   9  public record WriteSinkEvidence(
  10      UUID operationId,
  11      WriteSinkId sink,
  12      TargetEpoch targetEpoch,
  13      PolicyRevision policyRevision,
  14      Outcome outcome,
  15      String evidenceHash
  16  ) {
  17      public WriteSinkEvidence {
  18          Objects.requireNonNull(operationId, "operationId");
  19          Objects.requireNonNull(sink, "sink");
  20          Objects.requireNonNull(targetEpoch, "targetEpoch");
  21          Objects.requireNonNull(policyRevision, "policyRevision");
  22          Objects.requireNonNull(outcome, "outcome");
  23          evidenceHash = Objects.toString(evidenceHash, "").trim()
  24              .toLowerCase(Locale.ROOT);
  25          if (evidenceHash.isEmpty()) {
  26              throw new IllegalArgumentException("write sink evidence hash is required");
  27          }
  28      }
  29  
  30      public enum Outcome {
  31          ACCEPTED,
  32          REJECTED_STALE_EPOCH,
  33          REJECTED_POLICY,
  34          FENCED_UNVERIFIED
  35      }
  36  }
```

<a id="sink-extension"></a>
### `src/main/java/dev/ronova/coremod/root/api/WriteSinkExtension.java` · 1–6

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.extension.VersionedExtensionCodec;
   4  
   5  /** 7.16 seam; production sink evidence begins in 7.17. */
   6  public interface WriteSinkExtension extends VersionedExtensionCodec<WriteSinkEvidence> {}
```

<a id="completion-id"></a>
### `src/main/java/dev/ronova/coremod/root/api/CompletionContractId.java` · 1–18

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Locale;
   4  import java.util.Objects;
   5  import java.util.regex.Pattern;
   6  
   7  /** Stable identity for a non-CLEARED operation's finality contract. */
   8  public record CompletionContractId(String canonical) {
   9      private static final Pattern VALUE =
  10          Pattern.compile("[a-z0-9][a-z0-9_.:/-]{0,191}");
  11  
  12      public CompletionContractId {
  13          canonical = Objects.toString(canonical, "").trim().toLowerCase(Locale.ROOT);
  14          if (!VALUE.matcher(canonical).matches()) {
  15              throw new IllegalArgumentException("invalid completion contract id: " + canonical);
  16          }
  17      }
  18  }
```

<a id="completion-extension"></a>
### `src/main/java/dev/ronova/coremod/root/api/CompletionContractExtension.java` · 1–7

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.extension.VersionedExtensionCodec;
   4  
   5  /** 7.16 seam; production completion finality begins in 7.17. */
   6  public interface CompletionContractExtension
   7      extends VersionedExtensionCodec<CompletionEvaluation> {}
```

<a id="completion-evaluation"></a>
### `src/main/java/dev/ronova/coremod/root/api/CompletionEvaluation.java` · 1–31

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Objects;
   4  import java.util.UUID;
   5  import java.util.Locale;
   6  
   7  /** Current, revisioned evaluation; it is evidence and never a second Truth. */
   8  public record CompletionEvaluation(
   9      UUID operationId,
  10      CompletionContractId contract,
  11      long revision,
  12      boolean completionSatisfied,
  13      String evidenceHash,
  14      String detail
  15  ) {
  16      public CompletionEvaluation {
  17          Objects.requireNonNull(operationId, "operationId");
  18          Objects.requireNonNull(contract, "contract");
  19          if (revision <= 0L) {
  20              throw new IllegalArgumentException("completion revision must be positive");
  21          }
  22          evidenceHash = Objects.toString(evidenceHash, "").trim()
  23              .toLowerCase(Locale.ROOT);
  24          detail = Objects.toString(detail, "").trim();
  25          if (completionSatisfied && evidenceHash.isEmpty()) {
  26              throw new IllegalArgumentException(
  27                  "satisfied completion requires evidence hash"
  28              );
  29          }
  30      }
  31  }
```

<a id="extension-codec"></a>
### `src/main/java/dev/ronova/coremod/extension/VersionedExtensionCodec.java` · 1–14

```text
   1  package dev.ronova.coremod.extension;
   2  
   3  /** Codec registered before Root publication; decode must be deterministic. */
   4  public interface VersionedExtensionCodec<T> {
   5      VersionedExtensionId id();
   6  
   7      int schemaVersion();
   8  
   9      Class<T> valueType();
  10  
  11      VersionedExtensionEnvelope encode(T value, long revision);
  12  
  13      T decode(VersionedExtensionEnvelope envelope);
  14  }
```

<a id="writer-traversal"></a>
### `src/main/java/dev/ronova/coremod/observation/NbtWriteReferences.java` · 1–40

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import java.lang.ref.WeakReference;
   4  import java.util.*;
   5  
   6  /** Actual writer traversal; published entries retain neither holders nor their object graphs. */
   7  public final class NbtWriteReferences {
   8      public record Entry(WeakReference<Object> value,List<NbtStoragePreservation.Step> path) {
   9          public Entry {path=List.copyOf(path);}
  10      }
  11      public record Snapshot(boolean complete,List<Entry> entries) {
  12          public Snapshot {entries=List.copyOf(entries);}
  13      }
  14      private static final class Frame {Object value;int index;Frame(Object value){this.value=value;}}
  15      private final ArrayList<Frame> frames=new ArrayList<>();
  16      private final ArrayList<NbtStoragePreservation.Step> path=new ArrayList<>();
  17      private final ArrayList<Entry> entries=new ArrayList<>();
  18      private boolean valid=true;private int nodes;
  19      public NbtWriteReferences(Object root){frames.add(new Frame(root));}
  20      public AutoCloseable enter(Object parent,Object child,String key,boolean compound,boolean supported) {
  21          if(!valid)return null;
  22          if(!supported||frames.get(frames.size()-1).value!=parent||path.size()>=64||++nodes>4096
  23              ||key!=null&&key.length()>512){valid=false;return null;}
  24          for(var frame:frames)if(frame.value==child){valid=false;return null;}
  25          var step=key==null?new NbtStoragePreservation.Index(frames.get(frames.size()-1).index++):new NbtStoragePreservation.Key(key);
  26          path.add(step);var frame=new Frame(child);frames.add(frame);
  27          if(compound) {
  28              if(entries.size()>=64)valid=false;
  29              else entries.add(new Entry(new WeakReference<>(child),path));
  30          }
  31          return ()->{
  32              if(frames.isEmpty())return;
  33              if(frames.get(frames.size()-1)!=frame){valid=false;return;}
  34              frames.remove(frames.size()-1);path.remove(path.size()-1);frame.value=null;
  35          };
  36      }
  37      public Snapshot snapshot(){return valid&&frames.size()==1?new Snapshot(true,entries):new Snapshot(false,List.of());}
  38      /** Drop owned traversal references even if a caller retains its already-closed stream or scope. */
  39      public void release(){valid=false;for(var frame:frames)frame.value=null;frames.clear();path.clear();entries.clear();}
  40  }
```

<a id="storage-sequence"></a>
### `src/main/java/dev/ronova/coremod/observation/StorageMutationSequence.java` · 1–31

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import java.util.concurrent.atomic.AtomicLong;
   4  
   5  /** Observation stamp only; it never locks a foreign cache or grants permission to write it. */
   6  public final class StorageMutationSequence {
   7      private static final long ACTIVE_MASK = 0xffffL;
   8      private final AtomicLong state = new AtomicLong();
   9      public void begin() {
  10          for (;;) {
  11              long prior = state.get();
  12              if (prior < 0) return;
  13              long active = prior & ACTIVE_MASK;
  14              long next = active == ACTIVE_MASK || prior > Long.MAX_VALUE - 0x10001L
  15                  ? Long.MIN_VALUE : prior + 0x10001L;
  16              if (state.compareAndSet(prior, next)) return;
  17          }
  18      }
  19      public void end() {
  20          for (;;) {
  21              long prior = state.get();
  22              if (prior < 0) return;
  23              long next = (prior & ACTIVE_MASK) == 0 ? Long.MIN_VALUE : prior - 1;
  24              if (state.compareAndSet(prior, next)) return;
  25          }
  26      }
  27      public long observationVersion() {
  28          long current = state.get();
  29          return current < 0 || (current & ACTIVE_MASK) != 0 ? -1 : current;
  30      }
  31  }
```

<a id="encoded-write"></a>
### `src/main/java/dev/ronova/coremod/observation/SavedDataEncodedWrite.java` · 1–101

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import java.io.*;
   4  import java.security.*;
   5  import java.util.HexFormat;
   6  
   7  /** Bounded digest of bytes accepted by the actual compressed output stream, not a commit receipt. */
   8  public final class SavedDataEncodedWrite implements AutoCloseable {
   9      private static final ThreadLocal<SavedDataEncodedWrite> CURRENT=new ThreadLocal<>();
  10      private final SavedDataEncodedWrite previous;
  11      private final SavedDataSaveTrace trace;
  12      private final long generation;
  13      private Object image;
  14      private Object holder;
  15      private final Thread owner=Thread.currentThread();
  16      private final MessageDigest digest;
  17      private final NbtWriteReferences references;
  18      private boolean bound,closed,successful;
  19      private volatile boolean invalid;
  20      private long bytes;
  21      private String fileIdentity;
  22      private boolean fileIdentityConfirmed;
  23      private SavedDataEncodedWrite(SavedDataSaveTrace trace,Object image,Object holder) {
  24          this.trace=trace;this.image=image;this.holder=holder;references=new NbtWriteReferences(image);generation=trace.snapshot().generation();previous=CURRENT.get();
  25          if(previous!=null)previous.invalid=true;
  26          try {digest=MessageDigest.getInstance("SHA-256");} catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
  27          CURRENT.set(this);
  28      }
  29      public static SavedDataEncodedWrite open(SavedDataSaveTrace trace,Object image){return open(trace,image,null);}
  30      public static SavedDataEncodedWrite open(SavedDataSaveTrace trace,Object image,Object holder){return trace==null?null:new SavedDataEncodedWrite(trace,image,holder);}
  31      public static AutoCloseable enterReference(Object parent,net.minecraft.nbt.Tag child,String key) {
  32          var capture=CURRENT.get();if(capture==null||capture.invalid||capture.closed)return null;
  33          Class<?> type=child==null?null:child.getClass();
  34          boolean compound=type==net.minecraft.nbt.CompoundTag.class;
  35          boolean supported=compound||type==net.minecraft.nbt.ListTag.class||type==net.minecraft.nbt.ByteTag.class
  36              ||type==net.minecraft.nbt.ShortTag.class||type==net.minecraft.nbt.IntTag.class||type==net.minecraft.nbt.LongTag.class
  37              ||type==net.minecraft.nbt.FloatTag.class||type==net.minecraft.nbt.DoubleTag.class||type==net.minecraft.nbt.StringTag.class
  38              ||type==net.minecraft.nbt.ByteArrayTag.class||type==net.minecraft.nbt.IntArrayTag.class||type==net.minecraft.nbt.LongArrayTag.class;
  39          return capture.references.enter(parent,child,key,compound,supported);
  40      }
  41      public static void leaveReference(AutoCloseable scope) {
  42          if(scope!=null)try {scope.close();}catch(Exception unavailable){var capture=CURRENT.get();if(capture!=null)capture.invalidate();}
  43      }
  44      public static OutputStream observe(Object image,OutputStream stream) {
  45          var capture=CURRENT.get();
  46          if(capture==null)return stream;
  47          if(capture.image!=image||capture.bound||capture.closed){capture.invalid=true;return stream;}
  48          capture.bound=true;
  49          // Only the exact JDK stream is supported. Wrappers are not traversed by
  50          // guessed fields and a synthetic stream can never confer a file identity.
  51          final FileOutputStream file=stream.getClass()==FileOutputStream.class?(FileOutputStream)stream:null;
  52          if(file!=null)try {capture.fileIdentity=dev.ronova.coremod.nativebridge.NativeBridge.savedFileIdentity(file.getFD());}
  53          catch(IOException|RuntimeException|LinkageError unavailable){capture.fileIdentity=null;}
  54          return new FilterOutputStream(stream) {
  55              @Override public void write(int value)throws IOException {
  56                  try {out.write(value);}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
  57                  synchronized(capture){if(capture.accept(1))capture.digest.update((byte)value);}
  58              }
  59              @Override public void write(byte[] value,int offset,int length)throws IOException {
  60                  try {out.write(value,offset,length);}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
  61                  synchronized(capture){if(capture.accept(length))capture.digest.update(value,offset,length);}
  62              }
  63              @Override public void flush()throws IOException {
  64                  try {out.flush();}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
  65              }
  66              @Override public void close()throws IOException {
  67                  if(file!=null&&!capture.fileIdentityConfirmed)try {
  68                      String after=dev.ronova.coremod.nativebridge.NativeBridge.savedFileIdentity(file.getFD());
  69                      capture.fileIdentityConfirmed=capture.fileIdentity!=null&&capture.fileIdentity.equals(after);
  70                  } catch(IOException|RuntimeException|LinkageError unavailable){capture.fileIdentityConfirmed=false;}
  71                  try {out.close();}catch(IOException|RuntimeException|Error failure){capture.invalidate();throw failure;}
  72              }
  73          };
  74      }
  75      private synchronized void invalidate(){invalid=true;trace.invalidateEncoded(generation);}
  76      private boolean accept(int count){
  77          if(Thread.currentThread()!=owner||closed){invalidate();return false;}
  78          if(invalid)return false;
  79          if(count<0||bytes>16L*1024*1024-count){invalid=true;return false;}
  80          bytes+=count;return true;
  81      }
  82      public synchronized void succeeded(){if(Thread.currentThread()!=owner)invalidate();else successful=true;}
  83      @Override public synchronized void close(){
  84          if(Thread.currentThread()!=owner){invalidate();return;}
  85          if(closed)return;closed=true;
  86          if(CURRENT.get()!=this){invalid=true;releaseReferences();return;}
  87          var restore=previous;while(restore!=null&&restore.closed)restore=restore.previous;
  88          if(restore==null)CURRENT.remove();else CURRENT.set(restore);
  89          try {if(successful&&bound&&!invalid) {
  90              var encoded=trace.encoded(generation,HexFormat.of().formatHex(digest.digest()),bytes,references.snapshot(),
  91                  fileIdentityConfirmed?fileIdentity:null);
  92              try {
  93                  if(encoded!=null&&holder instanceof net.minecraft.world.level.saveddata.SavedData data)
  94                      dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.savedDataEncoded(data,encoded);
  95              } catch(RuntimeException|LinkageError unavailable) {
  96                  // Absence of a source confirmation must not suppress the original save.
  97              } finally {java.lang.ref.Reference.reachabilityFence(image);}
  98          }} finally {releaseReferences();}
  99      }
 100      private void releaseReferences(){references.release();image=null;holder=null;}
 101  }
```

<a id="deferred-receipts"></a>
### `src/main/java/dev/ronova/coremod/observation/DeferredReceiptBuffer.java` · 1–95

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import java.util.ArrayDeque;
   4  import java.util.Objects;
   5  import java.util.function.BiPredicate;
   6  import java.util.function.BooleanSupplier;
   7  import java.util.function.Supplier;
   8  
   9  /** Reserve result retention before starting a mutation; retry only the receipt, never the mutation. */
  10  final class DeferredReceiptBuffer<K, V> implements AutoCloseable {
  11      private final int capacity;
  12      private final Supplier<Runnable> reserve;
  13      private final ArrayDeque<Ticket> rotation = new ArrayDeque<>();
  14      private boolean closed;
  15      DeferredReceiptBuffer(int capacity, Supplier<Runnable> reserve) {
  16          if (capacity < 1) throw new IllegalArgumentException("invalid receipt capacity");
  17          this.capacity = capacity; this.reserve = Objects.requireNonNull(reserve);
  18      }
  19      final class Ticket implements AutoCloseable {
  20          final K key;
  21          private final Runnable release;
  22          private V result;
  23          private boolean released;
  24          private Ticket(K key, Runnable release) { this.key = key; this.release = release; }
  25          void complete(V result) {
  26              synchronized (DeferredReceiptBuffer.this) {
  27                  if (released) return; // Shutdown preserves the pre-existing durable unknown intent.
  28                  if (this.result != null) throw new IllegalStateException("receipt already completed");
  29                  this.result = Objects.requireNonNull(result);
  30              }
  31          }
  32          @Override public void close() {
  33              synchronized (DeferredReceiptBuffer.this) {
  34                  if (released) return;
  35                  released = true; rotation.remove(this); release.run();
  36              }
  37          }
  38      }
  39      synchronized Ticket reserve(K key) {
  40          Objects.requireNonNull(key);
  41          if (closed || rotation.size() >= capacity) return null;
  42          Runnable release = reserve.get();
  43          if (release == null) return null;
  44          var ticket = new Ticket(key, release); rotation.addLast(ticket); return ticket;
  45      }
  46      synchronized int flush(int limit, BooleanSupplier budget, BiPredicate<K, V> publish) {
  47          if (closed || limit < 1) return 0;
  48          int visited = rotation.size(), attempted = 0;
  49          while (visited-- > 0 && attempted < limit && !rotation.isEmpty()) {
  50              var ticket = rotation.removeFirst(); rotation.addLast(ticket);
  51              if (ticket.result == null) continue;
  52              if (!budget.getAsBoolean()) break;
  53              attempted++;
  54              boolean accepted;
  55              try { accepted = publish.test(ticket.key, ticket.result); }
  56              catch (RuntimeException | LinkageError unavailable) { accepted = false; }
  57              if (accepted) ticket.close();
  58          }
  59          return attempted;
  60      }
  61      synchronized int size() { return rotation.size(); }
  62      /** A shutdown snapshot includes only already-completed work. Failure retains every ticket. */
  63      synchronized boolean flushBatch(java.util.function.Predicate<java.util.List<java.util.Map.Entry<K, V>>> publish) {
  64          if (closed) return false;
  65          var ready = rotation.stream().filter(ticket -> ticket.result != null).toList();
  66          if (ready.isEmpty()) return true;
  67          var values = ready.stream().map(ticket -> java.util.Map.entry(ticket.key, ticket.result)).toList();
  68          boolean accepted;
  69          try { accepted = publish.test(values); }
  70          catch (RuntimeException | LinkageError unavailable) { accepted = false; }
  71          if (!accepted) return false;
  72          RuntimeException failure = null;
  73          for (var ticket : ready) try { ticket.close(); }
  74          catch (RuntimeException error) {
  75              if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
  76          }
  77          if (failure != null) throw failure;
  78          return true;
  79      }
  80      synchronized int readyCount() { return (int) rotation.stream().filter(ticket -> ticket.result != null).count(); }
  81      synchronized boolean anyKey(java.util.function.Predicate<K> predicate) {
  82          return rotation.stream().anyMatch(ticket -> predicate.test(ticket.key));
  83      }
  84      @Override public synchronized void close() {
  85          if (closed) return; closed = true;
  86          RuntimeException failure = null;
  87          while (!rotation.isEmpty()) {
  88              try { rotation.peekFirst().close(); }
  89              catch (RuntimeException error) {
  90                  if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
  91              }
  92          }
  93          if (failure != null) throw failure;
  94      }
  95  }
```

<a id="readback-query"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoveryStorageReadbackQuery.java` · 1–32

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import dev.ronova.coremod.root.api.*;
   4  import java.util.Objects;
   5  import java.util.function.Function;
   6  
   7  /** Query one acknowledged original intent. No new intent, write permission or completion fact is created. */
   8  public final class RecoveryStorageReadbackQuery {
   9      private RecoveryStorageReadbackQuery() {}
  10      public static RecoveryFileReadback.Result query(EffectKey key,
  11          Function<EffectKey,RecoveryWorkResult> lookup,
  12          Function<RecoveryWorkDescriptor,RecoveryFileReadback.Result> readCurrentBinding) {
  13          Objects.requireNonNull(lookup);Objects.requireNonNull(readCurrentBinding);
  14          if(key==null)return unavailable();
  15          var original=lookup.apply(key);
  16          if(original==null||!key.equals(original.record().key())
  17              ||original.descriptor().kind()!=RecoveryWorkDescriptor.Kind.STORAGE_COMMIT)return unavailable();
  18          var observed=readCurrentBinding.apply(original.descriptor());
  19          // ACK loss, replacement of the original record, or a concurrent result update requires a fresh query.
  20          if(observed==null||!original.equals(lookup.apply(key)))return unavailable();
  21          if(observed.status()==RecoveryFileReadback.Status.OBSERVED_MATCH
  22              &&(!original.descriptor().expectedHash().equals(observed.observedHash())
  23                  ||observed.reason()!=RecoveryFileReadback.Reason.NONE||observed.bytes()<0
  24                  ||observed.fileIdentity()==null||observed.fileIdentity().isEmpty()
  25                  ||!original.descriptor().fileIdentity().isEmpty()&&!original.descriptor().fileIdentity().equals(observed.fileIdentity())))return unavailable();
  26          return observed;
  27      }
  28      private static RecoveryFileReadback.Result unavailable() {
  29          return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
  30              RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
  31      }
  32  }
```

<a id="task-event-run"></a>
### `src/main/java/dev/ronova/coremod/observation/TaskEventRun.java` · 1–37

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import java.util.Objects;
   4  
   5  /** Consecutive, already receiver-verified events for one task. Never spans another
   6   * task, a copy event, a different parent/scheduler, or a changed proof generation.
   7   * Only exactly paired exits become completed counts; one open token is retained. */
   8  final class TaskEventRun {
   9      final Object task,parent;
  10      final long changes,dropped;
  11      Object scheduler;
  12      long completed,submissions,execution,events=1;
  13      boolean running=true,hasScheduler;
  14      TaskEventRun(Object task,Object parent,long execution,long changes,long dropped) {
  15          this.task=Objects.requireNonNull(task);this.parent=parent==task?null:parent;
  16          if(execution<1)throw new IllegalArgumentException("invalid execution token");
  17          this.execution=execution;this.changes=changes;this.dropped=dropped;
  18      }
  19      boolean append(int kind,Object task,Object scheduler,Object parent,long token,long changes,long dropped) {
  20          if(this.task!=task||this.parent!=(parent==task?null:parent)||this.changes!=changes||this.dropped!=dropped||events==Long.MAX_VALUE)return false;
  21          if(kind==1) {
  22              if(running||token<=execution)return false;
  23              execution=token;running=true;
  24          } else if(kind==2) {
  25              if(!running||token!=execution||completed==Long.MAX_VALUE)return false;
  26              completed++;running=false;
  27          } else if(kind==3) {
  28              if(token!=0||submissions==Long.MAX_VALUE||hasScheduler&&this.scheduler!=scheduler)return false;
  29              this.scheduler=scheduler;hasScheduler=true;submissions++;
  30          } else return false;
  31          events++;return true;
  32      }
  33      void mergeInto(TaskExecutionGraph graph) {
  34          graph.observedCompletedRun(task,scheduler,parent,completed,submissions);
  35          if(running)graph.entered(task,execution,parent);
  36      }
  37  }
```

<a id="module-contract"></a>
### `src/main/java/dev/ronova/coremod/capability/ServerCapabilityModule.java` · 1–10

```text
   1  package dev.ronova.coremod.capability;
   2  
   3  import dev.ronova.coremod.contract.Capability;
   4  
   5  /** One isolated server capability implementation. Modules never register their own tick loop. */
   6  public interface ServerCapabilityModule {
   7      Capability capability();
   8  
   9      void repair(CapabilityContext context);
  10  }
```

<a id="module-runtime"></a>
### `src/main/java/dev/ronova/coremod/capability/CapabilityRuntime.java` · 1–126

```text
   1  package dev.ronova.coremod.capability;
   2  
   3  import dev.ronova.coremod.capability.protection.LifecycleContinuityModule;
   4  import dev.ronova.coremod.capability.protection.TopologyContinuityModule;
   5  import dev.ronova.coremod.capability.recovery.BladeContinuityModule;
   6  import dev.ronova.coremod.capability.recovery.NegativeEffectCleanseModule;
   7  import dev.ronova.coremod.capability.recovery.VitalsRecoveryModule;
   8  import dev.ronova.coremod.capability.utility.FlightModule;
   9  import dev.ronova.coremod.contract.DominionDecision;
  10  import dev.ronova.coremod.observation.TopologyObservation;
  11  import java.util.List;
  12  import java.util.Objects;
  13  import net.minecraft.server.level.ServerPlayer;
  14  import net.minecraft.world.entity.player.Player;
  15  
  16  /** Explicit module dispatcher. It is the only tick/repair owner for server capabilities. */
  17  public final class CapabilityRuntime {
  18      private final AuthorityRuntimeState authorityState;
  19      private final TopologyObservation observation;
  20      private final List<ServerCapabilityModule> modules;
  21      private final CapabilityDiagnostics diagnostics = new CapabilityDiagnostics();
  22      private final ThreadLocal<Boolean> repairing = ThreadLocal.withInitial(() -> false);
  23  
  24      private CapabilityRuntime(
  25          AuthorityRuntimeState authorityState,
  26          TopologyObservation observation,
  27          List<ServerCapabilityModule> modules
  28      ) {
  29          this.authorityState = authorityState;
  30          this.observation = observation;
  31          this.modules = List.copyOf(modules);
  32      }
  33  
  34      public static CapabilityRuntime standard(AuthorityRuntimeState authorityState) {
  35          return standard(authorityState, null);
  36      }
  37  
  38      public static CapabilityRuntime standard(
  39          AuthorityRuntimeState authorityState,
  40          TopologyObservation observation
  41      ) {
  42          return new CapabilityRuntime(authorityState, observation, List.of(
  43              new LifecycleContinuityModule(),
  44              new VitalsRecoveryModule(),
  45              new NegativeEffectCleanseModule(),
  46              new FlightModule(),
  47              new TopologyContinuityModule(),
  48              new BladeContinuityModule()
  49          ));
  50      }
  51  
  52      public void repair(Player player, DominionDecision.Authority authority) {
  53          if (player == null || authority == null || !authority.authorized() || repairing.get()) {
  54              return;
  55          }
  56          int tick = tick(player);
  57          if (authorityState != null) {
  58              authorityState.markRepaired(player, tick);
  59          }
  60          boolean topologyBroken = topologyBroken(player);
  61          repairing.set(true);
  62          try {
  63              for (ServerCapabilityModule module : modules) {
  64                  if (!authority.capabilities().contains(module.capability())) {
  65                      continue;
  66                  }
  67                  CapabilityContext context = new CapabilityContext(
  68                      player, module.capability(), authorityState, observation,
  69                      diagnostics, topologyBroken
  70                  );
  71                  try {
  72                      module.repair(context);
  73                  } catch (Throwable failure) {
  74                      diagnostics.record(module.capability());
  75                  }
  76              }
  77          } finally {
  78              repairing.set(false);
  79          }
  80      }
  81  
  82      public void maintain(Player player, DominionDecision.Authority authority) {
  83          if (player == null) {
  84              return;
  85          }
  86          if (authorityState == null
  87              || authorityState.claimRoutineRepair(player, tick(player))) {
  88              repair(player, authority);
  89          }
  90      }
  91  
  92      public void maintainAnchors(java.util.function.Function<Player, DominionDecision.Authority> resolver) {
  93          if (authorityState == null || authorityState.shuttingDown()) {
  94              return;
  95          }
  96          Objects.requireNonNull(resolver, "resolver");
  97          for (ServerPlayer player : authorityState.anchors()) {
  98              if (player.server == authorityState.server()) {
  99                  try {
 100                  var anchor=authorityState.anchorId(player);
 101                  if(anchor==null)continue;
 102                  dev.ronova.coremod.capability.recovery.RegisteredOwnerIdentity.repair(player,anchor);
 103                  var authority=resolver.apply(player);
 104                  if(!authorityState.shuttingDown()&&authorityState.anchor(anchor)==player)repair(player,authority);
 105                  } catch(RuntimeException|LinkageError failure) {
 106                      diagnostics.record(dev.ronova.coremod.contract.Capability.TOPOLOGY_CONTINUITY);
 107                  }
 108              }
 109          }
 110      }
 111  
 112      public CapabilityDiagnostics diagnostics() { return diagnostics; }
 113  
 114      private static int tick(Player player) {
 115          return player instanceof ServerPlayer serverPlayer && serverPlayer.server != null
 116              ? serverPlayer.server.getTickCount() : player.tickCount;
 117      }
 118  
 119      private static boolean topologyBroken(Player player) {
 120          try {
 121              return player.isRemoved() || !player.isAddedToWorld();
 122          } catch (Throwable ignored) {
 123              return true;
 124          }
 125      }
 126  }
```

<a id="module-context"></a>
### `src/main/java/dev/ronova/coremod/capability/CapabilityContext.java` · 1–48

```text
   1  package dev.ronova.coremod.capability;
   2  
   3  import dev.ronova.coremod.contract.Capability;
   4  import dev.ronova.coremod.observation.TopologyObservation;
   5  import java.util.Objects;
   6  import net.minecraft.world.entity.player.Player;
   7  
   8  /** Per-module execution context with counted best-effort compatibility operations. */
   9  public final class CapabilityContext {
  10      private final Player player;
  11      private final Capability capability;
  12      private final AuthorityRuntimeState authorityState;
  13      private final TopologyObservation observation;
  14      private final CapabilityDiagnostics diagnostics;
  15      private final boolean topologyBroken;
  16  
  17      CapabilityContext(
  18          Player player,
  19          Capability capability,
  20          AuthorityRuntimeState authorityState,
  21          TopologyObservation observation,
  22          CapabilityDiagnostics diagnostics,
  23          boolean topologyBroken
  24      ) {
  25          this.player = Objects.requireNonNull(player, "player");
  26          this.capability = Objects.requireNonNull(capability, "capability");
  27          this.authorityState = authorityState;
  28          this.observation = observation;
  29          this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  30          this.topologyBroken = topologyBroken;
  31      }
  32  
  33      public Player player() { return player; }
  34  
  35      public AuthorityRuntimeState authorityState() { return authorityState; }
  36  
  37      public TopologyObservation observation() { return observation; }
  38  
  39      public boolean topologyBroken() { return topologyBroken; }
  40  
  41      public void attempt(Runnable operation) {
  42          try {
  43              operation.run();
  44          } catch (Throwable failure) {
  45              diagnostics.record(capability);
  46          }
  47      }
  48  }
```

<a id="module-diagnostics"></a>
### `src/main/java/dev/ronova/coremod/capability/CapabilityDiagnostics.java` · 1–25

```text
   1  package dev.ronova.coremod.capability;
   2  
   3  import dev.ronova.coremod.contract.Capability;
   4  import java.util.concurrent.atomic.AtomicLongArray;
   5  
   6  /** Counted compatibility-boundary failures; capability recovery never fails silently. */
   7  public final class CapabilityDiagnostics {
   8      private final AtomicLongArray failures = new AtomicLongArray(Capability.slotCount());
   9  
  10      void record(Capability capability) {
  11          failures.incrementAndGet(capability.stableId());
  12      }
  13  
  14      public long failures(Capability capability) {
  15          return capability == null ? 0L : failures.get(capability.stableId());
  16      }
  17  
  18      public long totalFailures() {
  19          long total = 0L;
  20          for (Capability capability : Capability.values()) {
  21              total += failures(capability);
  22          }
  23          return total;
  24      }
  25  }
```

<a id="capability-bits"></a>
### `src/main/java/dev/ronova/coremod/contract/Capability.java` · 1–35

```text
   1  package dev.ronova.coremod.contract;
   2  
   3  /** Stable capability bits. IDs are protocol data and never depend on declaration order. */
   4  public enum Capability {
   5      LIFECYCLE_CONTINUITY(0),
   6      TOPOLOGY_CONTINUITY(1),
   7      VITALS_RECOVERY(2),
   8      NEGATIVE_EFFECT_CLEANSE(3),
   9      FLIGHT(4),
  10      BLADE_CONTINUITY(5),
  11      CONNECTION_CONTINUITY(6),
  12      PRIMARY_ACTION(7),
  13      AREA_ACTION(8),
  14      LOADED_ACTION(9),
  15      INSTANT_MINING(10);
  16  
  17      private final int stableId;
  18  
  19      Capability(int stableId) {
  20          if (stableId < 0 || stableId >= Long.SIZE) {
  21              throw new IllegalArgumentException("capability id outside long mask: " + stableId);
  22          }
  23          this.stableId = stableId;
  24      }
  25  
  26      public int stableId() { return stableId; }
  27  
  28      public static int slotCount() {
  29          int highest = 0;
  30          for (Capability capability : values()) {
  31              highest = Math.max(highest, capability.stableId);
  32          }
  33          return highest + 1;
  34      }
  35  }
```

<a id="surface-maturity"></a>
### `src/main/java/dev/ronova/coremod/surface/SurfaceRegistry.java` · 1–55

```text
   1  package dev.ronova.coremod.surface;
   2  
   3  import java.util.*;
   4  
   5  /** Reports all eight categories; an observation-only adapter cannot claim enforcement or FULL. */
   6  public final class SurfaceRegistry {
   7      public enum Maturity { S0_ABSENT, S1_OBSERVE, S2_ATTRIBUTE, S3_ENFORCE, S4_REPAIR, S5_RECOVER, S6_COMPLETE }
   8      public enum State { UNAVAILABLE, MONITOR, FAILED }
   9      public record Report(RuntimeSurface surface, Maturity supported, Maturity evidence, State state,
  10          long registryRevision, long sampledRevision, SurfaceAdapter.Observation lastTrustedObservation, String diagnostic) {}
  11      private final EnumMap<RuntimeSurface, SurfaceAdapter> adapters = new EnumMap<>(RuntimeSurface.class);
  12      private final EnumMap<RuntimeSurface, SurfaceAdapter.Observation> trusted = new EnumMap<>(RuntimeSurface.class);
  13      private final EnumMap<RuntimeSurface, String> failures = new EnumMap<>(RuntimeSurface.class);
  14      private final EnumMap<RuntimeSurface, Long> registrations = new EnumMap<>(RuntimeSurface.class);
  15      private final EnumMap<RuntimeSurface, Long> trustedRegistrations = new EnumMap<>(RuntimeSurface.class);
  16      private long revision;
  17      private long sampledRevision;
  18      public synchronized void register(SurfaceAdapter adapter) {
  19          Objects.requireNonNull(adapter);
  20          RuntimeSurface surface = Objects.requireNonNull(adapter.surface());
  21          if (adapters.containsKey(surface)) throw new IllegalArgumentException("surface adapter already registered");
  22          revision = Math.addExact(revision, 1); adapters.put(surface, adapter);
  23          registrations.put(surface, revision); failures.remove(surface);
  24      }
  25      public synchronized void remove(RuntimeSurface surface) {
  26          if (adapters.containsKey(surface)) { revision = Math.addExact(revision, 1); adapters.remove(surface); }
  27      }
  28      public synchronized void sample(SurfaceAdapter.Context context) {
  29          if (context.sampleRevision() <= sampledRevision) throw new IllegalArgumentException("non-monotonic surface observation");
  30          sampledRevision = context.sampleRevision();
  31          adapters.forEach((surface, adapter) -> {
  32              try {
  33                  var value = Objects.requireNonNull(adapter.observe(context), "missing observation");
  34                  if (!context.equals(value.context())) throw new IllegalArgumentException("observation context mismatch");
  35                  trusted.put(surface, value); trustedRegistrations.put(surface, registrations.get(surface)); failures.remove(surface);
  36              } catch (RuntimeException failed) {
  37                  String text = failed.getClass().getSimpleName() + ": " + Objects.toString(failed.getMessage(), "");
  38                  failures.put(surface, text.substring(0, Math.min(256, text.length())));
  39              }
  40          });
  41      }
  42      public synchronized List<Report> snapshot() {
  43          List<Report> rows = new ArrayList<>();
  44          for (RuntimeSurface surface : RuntimeSurface.values()) {
  45              boolean registered = adapters.containsKey(surface), observed = registered && trusted.containsKey(surface)
  46                  && Objects.equals(registrations.get(surface), trustedRegistrations.get(surface)) && !failures.containsKey(surface);
  47              rows.add(new Report(surface, registered ? Maturity.S1_OBSERVE : Maturity.S0_ABSENT,
  48                  observed ? Maturity.S1_OBSERVE : Maturity.S0_ABSENT,
  49                  !registered ? State.UNAVAILABLE : failures.containsKey(surface) ? State.FAILED : observed ? State.MONITOR : State.UNAVAILABLE,
  50                  revision, sampledRevision, trusted.get(surface), failures.getOrDefault(surface,
  51                      registered ? "Observation only; attribution, plan, execution and completion ports are not registered" : "No registered observer")));
  52          }
  53          return List.copyOf(rows);
  54      }
  55  }
```

<a id="shadow-results"></a>
### `src/main/java/dev/ronova/coremod/persistence/ShadowVerification.java` · 1–50

```text
   1  package dev.ronova.coremod.persistence;
   2  
   3  import dev.ronova.coremod.contract.DominionEpoch;
   4  import java.util.Objects;
   5  
   6  /** Result of one authoritative-SavedData to shadow comparison. */
   7  public record ShadowVerification(
   8      Status status,
   9      DominionEpoch expectedEpoch,
  10      DominionEpoch observedEpoch,
  11      String expectedHash,
  12      String observedHash,
  13      String detail
  14  ) {
  15      public ShadowVerification {
  16          status = Objects.requireNonNull(status, "status");
  17          expectedEpoch = Objects.requireNonNull(expectedEpoch, "expectedEpoch");
  18          observedEpoch = Objects.requireNonNull(observedEpoch, "observedEpoch");
  19          expectedHash = Objects.toString(expectedHash, "");
  20          observedHash = Objects.toString(observedHash, "");
  21          detail = Objects.toString(detail, "");
  22      }
  23  
  24      public boolean accepted() {
  25          return status == Status.VERIFIED
  26              || status == Status.MIRRORED
  27              || status == Status.RECEIPT_MIRRORED;
  28      }
  29  
  30      public boolean matches() {
  31          return status == Status.VERIFIED;
  32      }
  33  
  34      public boolean rejected() {
  35          return !accepted();
  36      }
  37  
  38      public enum Status {
  39          VERIFIED,
  40          MIRRORED,
  41          RECEIPT_MIRRORED,
  42          REALM_MISMATCH,
  43          ROLLBACK_REJECTED,
  44          STATE_CONFLICT,
  45          RECEIPT_DRIFT,
  46          READ_ONLY,
  47          FAILPOINT,
  48          INVALID_INPUT
  49      }
  50  }
```

<a id="transform-bridge"></a>
### `src/main/java/dev/ronova/coremod/agent/NativeTransformBridge.java` · 1–37

```text
   1  package dev.ronova.coremod.agent;
   2  
   3  /** Called from the native JVMTI ClassFileLoadHook. */
   4  public final class NativeTransformBridge {
   5      private static final RonovaTransformer TRANSFORMER = new RonovaTransformer("native-jvmti");
   6      private static final java.util.Map<String,String> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();
   7  
   8      private NativeTransformBridge() {}
   9      public static boolean verify(String internalName,byte[] classBytes,ClassLoader definingLoader) {
  10          if(JdkTaskBoundaryContract.registered(internalName)) {
  11              boolean verified=definingLoader==null&&JdkTaskBoundaryContract.enabled()&&JdkTaskBoundaryContract.verify(internalName,classBytes);
  12              if(verified)FAILURES.remove(internalName);else FAILURES.put(internalName,"jdk-task-platform-image-or-boundary-unverified");
  13              return verified;
  14          }
  15          if(!RonovaTransformer.isTarget(internalName))return false;
  16          boolean loader=definingLoader==NativeTransformBridge.class.getClassLoader();
  17          boolean verified=loader&&SemanticGateVerifier.verify(internalName,classBytes);
  18          if(verified)FAILURES.remove(internalName);
  19          else FAILURES.put(internalName,loader?SemanticGateVerifier.explainFailure(internalName,classBytes):"defining-loader-mismatch");
  20          return verified;
  21      }
  22      public static java.util.Map<String,String> verificationFailures(){return java.util.Map.copyOf(FAILURES);}
  23  
  24      public static byte[] transform(String internalName, byte[] classBytes) {
  25          if (!RonovaTransformer.shouldInspect(internalName, classBytes)) {
  26              return null;
  27          }
  28          return TRANSFORMER.transform(
  29              null,
  30              NativeTransformBridge.class.getClassLoader(),
  31              internalName,
  32              null,
  33              null,
  34              classBytes
  35          );
  36      }
  37  }
```

<a id="code-seal-probe"></a>
### `src/main/java/dev/ronova/coremod/integrity/CodeSealProbe.java` · 1–10

```text
   1  package dev.ronova.coremod.integrity;
   2  
   3  /** Stateless execution canary for the registered Native code-image guard. */
   4  public final class CodeSealProbe {
   5      private CodeSealProbe() {}
   6      public static long evaluate(long challenge) {
   7          long mixed=challenge^0x7170D15EA5E12345L;
   8          return ((mixed<<17)|(mixed>>>47))+0x13579BDF2468ACE0L;
   9      }
  10  }
```

<a id="writer-path-comparison"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` · 1690–1722

```text
1690      private String storagePreparationStatus(SourceKey key,StoragePreparation pending) {
1691          var source=sources.get(key);
1692          if(closed)return "TRACKER_CLOSED";
1693          if(runtime.authorityState().shuttingDown())return "SHUTDOWN";
1694          if(source==null)return "SOURCE_RETIRED";
1695          if(source.encoding!=pending.encoding())return "ENCODING_REPLACED";
1696          if(!current(source.terminal.qualification()))return "QUALIFICATION_CHANGED";
1697          if(!source.currentStorageWorld())return "WORLD_CHANGED";
1698          if(source.memoryDisposition!=null&&!source.memoryDisposition.acknowledgedWrite(
1699              runtime.durableStore().recoveryMutationResult(source.memoryDisposition)))return "MEMORY_RESULT_UNACKNOWLEDGED";
1700          var now=source.saveTrace.snapshot();var then=pending.encoding().atWrite();
1701          if(!now.traceId().equals(then.traceId())||now.generation()!=then.generation())return "SAVE_GENERATION_CHANGED";
1702          if(now.active()!=0||now.failed()||now.ambiguous())return "SAVE_UNSETTLED";
1703          var written=source.saveTrace.encodedWrite();
1704          if(written==null||written.generation()!=then.generation())return "WRITTEN_DIGEST_REVOKED";
1705          if(written.fileIdentity()==null)return "WRITER_FILE_IDENTITY_UNAVAILABLE";
1706          if(source.confirmedEncoding!=pending.encoding()||!written.hash().equals(source.confirmedWrittenHash))return "WRITER_POSITIONS_UNCONFIRMED";
1707          return source.stillBound()||source.recordBound(true)||source.emptyAtomicSlot()?"CURRENT":"BINDING_CHANGED";
1708      }
1709      private boolean writtenPositionsMatch(Source source,StorageEncodingObservation encoding,SavedDataSaveTrace.EncodedWrite written) {
1710          if(!written.references().complete()||!encoding.positions().complete())return false;
1711          var actual=new java.util.HashSet<java.util.List<NbtStoragePreservation.Step>>();
1712          for(var entry:written.references().entries()) {
1713              Object value=entry.value().get();if(value==null)return false;
1714              if(value==source.seed.get()){actual.add(entry.path());continue;}
1715              if(value instanceof CompoundTag tag) {
1716                  var copied=copies.resolve(tag);
1717                  if(copied.state()==CopyProvenanceIndex.State.RESOLVED&&copied.evidenceVerified()&&source.key.equals(copied.sourceKey()))
1718                      actual.add(entry.path());
1719              }
1720          }
1721          return !actual.isEmpty()&&actual.equals(new java.util.HashSet<>(encoding.positions().paths()));
1722      }
```

<a id="reserve-and-mutate"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` · 1295–1340

```text
1295          if (slot == null || slot.getClass() != java.util.concurrent.atomic.AtomicReference.class || seed == null
1296              || server == null || !server.isSameThread()) return;
1297          var store = runtime.durableStore();
1298          RecoverySnapshotPlan plan;
1299          try (var observation = runtime.enforcement().beginObservation(0x6000)) {
1300              if (observation == null || !observation.stillCurrent() || !source.stillBound()) return;
1301              plan = source.staticHolder
1302                  ? store.approveStaticSnapshotPlan(source.terminal, evidence, source.key.observedVersion(), true)
1303                  : store.approveLoadedSnapshotPlan(source.terminal, evidence, source.key.observedVersion(), true);
1304              if (plan == null || !observation.finish()) return;
1305          }
1306          var intent = new RecoveryMutationIntent(plan, source.mutationIncarnation, source.staticHolder
1307              ? RecoveryMutationIntent.Kind.STATIC_SNAPSHOT : RecoveryMutationIntent.Kind.LOADED_SNAPSHOT);
1308          if (!store.prepareRecoveryMutation(intent, evidence, source.stillBound(), source.location)) return;
1309          var receipt = mutationReceipts.reserve(intent);
1310          if (receipt == null) { gap(source.key.targetEpoch()); return; }
1311          var outcome = RecoveryMutationIntent.Observation.NOT_EXECUTED;
1312          // The durable write can outlive an observation ticket. Observe again instead of extending its lease.
1313          try (var observation = runtime.enforcement().beginObservation(0x6000)) {
1314              if (observation == null || !observation.stillCurrent() || !source.stillBound()) return;
1315              // The acknowledged original intent and reserved receipt retain unfinished
1316              // task/save debt before the first possible cell write.
1317              boolean bound = observation.stillCurrent() && source.stillBound();
1318              try (var lease = store.acquireRecoveryMutationWrite(intent, evidence, bound)) {
1319                  if (lease == null) return;
1320                  outcome = RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
1321                  var result = AtomicSnapshotReference.clear(slot, seed, () -> lease.valid(plan)
1322                      && !runtime.authorityState().shuttingDown() && observation.stillCurrent()
1323                      && source.stillBound() && current(source.terminal.qualification()) && store.recoveryMutationPrepared(intent));
1324                  if (result == AtomicSnapshotReference.ClearResult.CLEARED) {
1325                      boolean verified = source.emptyAtomicSlot() && lease.valid(plan) && observation.finish();
1326                      outcome = verified ? RecoveryMutationIntent.Observation.WRITE_OBSERVED : RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
1327                      if (verified && source.staticHolder) clearedStaticSnapshots = next(clearedStaticSnapshots);
1328                      else if (verified) clearedLoadedSnapshots = next(clearedLoadedSnapshots);
1329                      else unverifiedSnapshotWrites = next(unverifiedSnapshotWrites);
1330                      if(verified)source.memoryDisposition=intent;
1331                      // This subeffect cannot discharge record/task/save obligations. Keep the checkpoint debt,
1332                      // and preserve existing copy descendants after the original atomic holder is emptied.
1333                      detachSource(source);
1334                  } else if (result == AtomicSnapshotReference.ClearResult.NOT_CURRENT) {
1335                      outcome = RecoveryMutationIntent.Observation.NOT_CURRENT;
1336                      detachSource(source);
1337                  } else outcome = RecoveryMutationIntent.Observation.NOT_EXECUTED;
1338              }
1339          } finally { receipt.complete(outcome); }
1340      }
```

<a id="publish-receipts"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` · 963–973

```text
 963          if ((budget.tickId() & 1) != 0) flushMutationReceipts(budget);
 964      }
 965      private void flushMutationReceipts(WorkBudget budget) {
 966          int[] attempts = { 0 };
 967          try {
 968              mutationReceipts.flush(4, () -> {
 969                  if (!budget.tryConsumeCount(1)) return false;
 970                  attempts[0]++; return true;
 971              }, runtime.durableStore()::observeRecoveryMutation);
 972          } finally { if (attempts[0] > 0) budget.finish(attempts[0], 0); }
 973      }
```

<a id="registered-readback"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` · 1813–1854

```text
1813      /** Original Root intent supplies the expected image; live source identity must still match its registration. */
1814      public synchronized RecoveryFileReadback.Result readStorageIntent(EffectKey intentKey,SourceKey sourceKey,
1815          SavedDataSaveTrace.Snapshot beforeSave,WorkBudget budget) {
1816          return RecoveryStorageReadbackQuery.query(intentKey,runtime.durableStore()::recoveryWorkResult,description->{
1817              var source=sources.get(sourceKey);
1818              if(source==null||!description.location().equals(source.location)
1819                  ||!description.registration().equals(source.mutationIncarnation)
1820                  ||description.sourceVersion()!=source.key.observedVersion()
1821                  ||!intentKey.targetEpoch().equals(source.key.targetEpoch())
1822                  ||!intentKey.canonicalCommandId().equals(source.terminal.commandId()))
1823                  return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
1824                      RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
1825              return readObservedStorage(sourceKey,beforeSave,description.expectedHash(),budget);
1826          });
1827      }
1828      /** Read only the file actually observed for this loaded source. This does not write Root completion facts. */
1829      public synchronized RecoveryFileReadback.Result readObservedStorage(SourceKey key,
1830          SavedDataSaveTrace.Snapshot beforeSave,String expectedEncodedHash,WorkBudget budget) {
1831          var source=sources.get(key);
1832          var unavailable=new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
1833              RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
1834          if(source==null||source.saveTrace==null||budget==null||closed)return unavailable;
1835          var server=source.server.get();
1836          if(server==null||!server.isSameThread()||!current(source.terminal.qualification()))return unavailable;
1837          var saved=source.saveTrace.snapshot();
1838          if(!saved.observedWriteReturnAfter(beforeSave)||saved.observedPath()==null)return unavailable;
1839          java.util.function.BooleanSupplier bound=()->source.currentStorageWorld()
1840              &&(source.stillBound()||source.recordBound(true)||source.emptyAtomicSlot());
1841          var capacity=runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP,65536L,
1842              HostResourceDomain.THREAD_STACK,65536L,HostResourceDomain.FILE_DESCRIPTOR,2L,
1843              HostResourceDomain.PLATFORM_HANDLE,2L,HostResourceDomain.DIRECT_BUFFER,32768L,
1844              HostResourceDomain.JNI_JNA_NATIVE,4096L),HostResourceGovernor.Priority.RECOVERY);
1845          if(!capacity.granted())return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
1846              RecoveryFileReadback.Reason.RESOURCE_UNAVAILABLE,0,null);
1847          try(var retained=capacity.reservation();var proof=runtime.enforcement().beginObservation(0x6000)) {
1848              if(proof==null||!proof.stillCurrent()||!bound.getAsBoolean())return unavailable;
1849              var result=RecoveryFileReadback.read(java.nio.file.Path.of(saved.observedPath()),expectedEncodedHash,
1850                  RecoveryFileReadback.MAX_BYTES,()->budget.tryConsumeCount(1));
1851              if(!saved.equals(source.saveTrace.snapshot())||!current(source.terminal.qualification())
1852                  ||!bound.getAsBoolean()||!proof.finish())return unavailable;
1853              return result;
1854          }
```

<a id="copy-registration"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` · 545–555

```text
 545          this.runtime = Objects.requireNonNull(runtime);
 546          runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP,
 547              HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "recovery-source-observation/weak-sources-and-leased-cursors-v2");
 548          runtime.resources().declareAccounting(HostResourceDomain.THREAD,
 549              HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "record-mutation/worker-permits-until-body-finally");
 550          copies = new CopyProvenanceIndex<>(2048, this::reserve, this::loss);
 551          taskObserver = new RecoveryTaskObserver(reserve(512*384+1024*768+2048*192),runtime.enforcement(),this::mergeTaskCopy);
 552          taskDisposition=new RecoveryTaskDisposition(runtime.durableStore(),runtime.enforcement(),taskObserver.graph(),this::taskCandidate,this::reserve);
 553          mutationReceipts = new DeferredReceiptBuffer<>(SOURCE_LIMIT, () -> reserve(4096));
 554          worldAdmission = new RetiredWorldAdmission<>(SUBJECT_LIMIT, () -> reserve(256));
 555          RecoveryObservationScope.installHook();
```

<a id="copy-loss-consumer"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` · 1509–1510

```text
1509      private void loss(SourceKey key) { gap(key.targetEpoch()); }
1510      private void gap(TargetEpoch target) { gaps = next(gaps); runtime.obligations().discoveryGap(target); }
```

<a id="copy-index-baseline"></a>
### `src/main/java/dev/ronova/coremod/observation/CopyProvenanceIndex.java` · 1–37

```text
   1  package dev.ronova.coremod.observation;
   2  
   3  import java.lang.ref.ReferenceQueue;
   4  import java.lang.ref.WeakReference;
   5  import java.util.HashMap;
   6  import java.util.Map;
   7  import java.util.Objects;
   8  import java.util.function.Consumer;
   9  import java.util.function.IntFunction;
  10  
  11  /**
  12   * Object-identity evidence for an observed seed and its actual copies.
  13   * Equal content, UUIDs and class names never create a relationship. This index
  14   * observes data flow; its answer is neither a Judgment nor permission to write.
  15   */
  16  public final class CopyProvenanceIndex<K> implements AutoCloseable {
  17      public enum State { UNOBSERVED, RESOLVED, AMBIGUOUS, STALE }
  18      public record Resolution<K>(State state, K sourceKey, Object seed, int copyDepth, boolean evidenceVerified) {
  19          public Resolution(State state, K sourceKey, Object seed) { this(state, sourceKey, seed, 0, false); }
  20      }
  21      private static final int ENTRY_BYTES = 384;
  22      private static final int REAP_SLICE = 16;
  23      private final int capacity;
  24      private final IntFunction<Runnable> reserve;
  25      private final Consumer<K> coverageLoss;
  26      private final ReferenceQueue<Object> collected = new ReferenceQueue<>();
  27      private final Map<IdentityRef, Entry<K>> entries = new HashMap<>();
  28      private final Map<K, java.util.Set<IdentityRef>> bySource = new HashMap<>();
  29      private long copied, conflicts, rejected, reaped;
  30      private boolean closed;
  31  
  32      public CopyProvenanceIndex(int capacity, IntFunction<Runnable> reserve,
  33                                 Consumer<K> coverageLoss) {
  34          if (capacity < 1) throw new IllegalArgumentException("positive capacity required");
  35          this.capacity = capacity;
  36          this.reserve = Objects.requireNonNull(reserve);
  37          this.coverageLoss = Objects.requireNonNull(coverageLoss);
```

<a id="copy-index-resolution-loss"></a>
### `src/main/java/dev/ronova/coremod/observation/CopyProvenanceIndex.java` · 102–164

```text
 102      public synchronized Resolution<K> resolve(Object value) {
 103          if (closed || value == null) return new Resolution<>(State.UNOBSERVED, null, null);
 104          reap();
 105          Entry<K> entry = entries.get(new IdentityRef(value));
 106          if (entry == null) return new Resolution<>(State.UNOBSERVED, null, null);
 107          if (entry.ambiguous) return new Resolution<>(State.AMBIGUOUS, null, null);
 108          Object seed = entry.seed.get();
 109          // The shared weak-reference object is the lineage token. Collecting the original
 110          // seed does not erase a copy edge witnessed while its actual objects were alive.
 111          return new Resolution<>(seed == null && entry.copyDepth == 0 ? State.STALE : State.RESOLVED,
 112              entry.key, seed, entry.copyDepth, entry.evidenceVerified);
 113      }
 114  
 115      /** Does not retain copies or require the original seed to remain alive. */
 116      public synchronized boolean hasLiveCopies(K key) {
 117          reap();
 118          var references=bySource.get(Objects.requireNonNull(key));
 119          if(references==null)return false;
 120          for(var reference:references) {
 121              var entry=entries.get(reference);
 122              if(reference.get()!=null&&entry!=null&&entry.copyDepth>0)return true;
 123          }
 124          return false;
 125      }
 126  
 127      private boolean insert(Object value, K key, WeakReference<Object> seed, int depth, boolean evidenceVerified) {
 128          if (entries.size() >= capacity) { loss(key); return false; }
 129          Runnable release = reserve.apply(ENTRY_BYTES);
 130          if (release == null) { loss(key); return false; }
 131          boolean retained = false;
 132          IdentityRef reference = new IdentityRef(value, collected);
 133          try {
 134              entries.put(reference, new Entry<>(key, seed, release, depth, evidenceVerified));
 135              bySource.computeIfAbsent(key, ignored -> new java.util.HashSet<>()).add(reference);
 136              retained = true;
 137              return true;
 138          } finally {
 139              if (!retained) { entries.remove(reference); unlink(key, reference); release.run(); }
 140          }
 141      }
 142  
 143      private void conflict(Entry<K> existing, K other) {
 144          if (!existing.ambiguous) {
 145              existing.ambiguous = true;
 146              conflicts = increment(conflicts);
 147              coverageLoss.accept(existing.key);
 148          }
 149          if (!existing.key.equals(other)) coverageLoss.accept(other);
 150      }
 151  
 152      private void loss(K key) {
 153          rejected = increment(rejected);
 154          coverageLoss.accept(key);
 155      }
 156  
 157      private void reap() {
 158          for (int i = 0; i < REAP_SLICE; i++) {
 159              IdentityRef ref = (IdentityRef) collected.poll();
 160              if (ref == null) return;
 161              Entry<K> entry = entries.remove(ref);
 162              if (entry != null) { unlink(entry.key, ref); entry.release.run(); reaped = increment(reaped); }
 163          }
 164      }
```

<a id="copy-index-retirement"></a>
### `src/main/java/dev/ronova/coremod/observation/CopyProvenanceIndex.java` · 171–210

```text
 171      /** Retire only direct seed admission; already observed successors retain their own lineage. */
 172      public synchronized void retireSeeds(K key) {
 173          Objects.requireNonNull(key);
 174          var references=bySource.get(key);
 175          if(references==null)return;
 176          RuntimeException failure=null;
 177          var iterator=references.iterator();
 178          while(iterator.hasNext()) {
 179              var reference=iterator.next();var entry=entries.get(reference);
 180              if(entry==null){iterator.remove();continue;}
 181              if(entry.copyDepth!=0)continue;
 182              iterator.remove();entries.remove(reference);
 183              try {entry.release.run();} catch(RuntimeException error) {
 184                  if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);
 185              }
 186          }
 187          if(references.isEmpty())bySource.remove(key);
 188          if(failure!=null)throw failure;
 189      }
 190      /** Retire a source revision without making the same surviving seed inherit its old authority. */
 191      public synchronized void forget(K key) {
 192          Objects.requireNonNull(key);
 193          var references = bySource.remove(key);
 194          RuntimeException failure = null;
 195          if (references != null) for (var reference : references) {
 196              Entry<K> entry = entries.remove(reference);
 197              if (entry != null) try { entry.release.run(); }
 198              catch (RuntimeException error) {
 199                  if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
 200              }
 201          }
 202          if (failure != null) throw failure;
 203      }
 204      private void unlink(K key, IdentityRef ref) {
 205          var references = bySource.get(key);
 206          if (references != null) { references.remove(ref); if (references.isEmpty()) bySource.remove(key); }
 207      }
 208  
 209      @Override public synchronized void close() {
 210          if (closed) return;
```

<a id="task-loss-consumer"></a>
### `src/main/java/dev/ronova/coremod/observation/RecoveryTaskObserver.java` · 155–189

```text
 155          if(!originSlots.tryAcquire()){lost();return;}
 156          if(origins.putIfAbsent(key,origin)==null)originScan.offer(key);else {originSlots.release();lost();}
 157      }
 158      void publish(Object value,Origin origin) {
 159          requireOwner();if(closed||value==null)return;
 160          Identity key=new Identity(value);
 161          if(origin==null){if(origins.remove(key)!=null)originSlots.release();return;}
 162          if(origins.replace(key,origin)!=null)return;
 163          if(!originSlots.tryAcquire()){lost();return;}
 164          if(origins.putIfAbsent(key,origin)==null)originScan.offer(key);else originSlots.release();
 165      }
 166      public int merge(int allowance) {
 167          requireOwner();if(allowance<0||allowance>64)throw new IllegalArgumentException("invalid task merge allowance");
 168          if(closed)return 0;
 169          for(int i=0;i<16;i++) {
 170              Identity collected=(Identity)COLLECTED_EXECUTIONS.poll();if(collected==null)break;
 171              if(EXECUTIONS.remove(collected)!=null)EXECUTION_SLOTS.release();
 172          }
 173          if(!install()){graph.evidenceLost();return 0;}
 174          long lost=LOST.get();if(lost!=observedLosses){graph.evidenceLost();observedLosses=lost;}
 175          int count=0;Captured captured;
 176          while(count<allowance&&(captured=poll())!=null) {
 177              count++;
 178              if(captured instanceof Copy copy){copyMerge.accept(copy);merged++;continue;}
 179              if(captured instanceof Run run) {
 180                  var proof=NativeBridge.taskObservers();var value=run.value;
 181                  if(!proof.covers(63)||proof.changes()!=value.changes||value.dropped!=observedDrops) {
 182                      observedDrops=value.dropped;graph.evidenceLost();continue;
 183                  }
 184                  value.mergeInto(graph);merged=Math.addExact(merged,value.events);continue;
 185              }
 186              Event event=(Event)captured;var proof=NativeBridge.taskObservers();
 187              Object task=event.task;
 188              if(task==null||!proof.covers(63)||proof.changes()!=event.changes||event.dropped!=observedDrops) {
 189                  observedDrops=event.dropped;graph.evidenceLost();continue;
```

<a id="actual-transformer"></a>
### `src/main/java/dev/ronova/coremod/agent/RonovaTransformer.java` · 1–126

```text
   1  package dev.ronova.coremod.agent;
   2  
   3  import java.lang.instrument.ClassFileTransformer;
   4  import java.security.ProtectionDomain;
   5  import java.util.Set;
   6  import org.objectweb.asm.ClassReader;
   7  import org.objectweb.asm.ClassWriter;
   8  import org.objectweb.asm.Opcodes;
   9  import org.objectweb.asm.Type;
  10  import org.objectweb.asm.tree.AbstractInsnNode;
  11  import org.objectweb.asm.tree.ClassNode;
  12  import org.objectweb.asm.tree.FrameNode;
  13  import org.objectweb.asm.tree.FieldInsnNode;
  14  import org.objectweb.asm.tree.InsnList;
  15  import org.objectweb.asm.tree.InsnNode;
  16  import org.objectweb.asm.tree.JumpInsnNode;
  17  import org.objectweb.asm.tree.LabelNode;
  18  import org.objectweb.asm.tree.MethodInsnNode;
  19  import org.objectweb.asm.tree.MethodNode;
  20  import org.objectweb.asm.tree.TypeInsnNode;
  21  import org.objectweb.asm.tree.VarInsnNode;
  22  
  23  final class RonovaTransformer implements ClassFileTransformer {
  24      private static final String ENTITY = "net/minecraft/world/entity/Entity";
  25      private static final String LIVING = "net/minecraft/world/entity/LivingEntity";
  26      private static final String LEVEL = "net/minecraft/world/level/Level";
  27      private static final String SERVER_LEVEL = "net/minecraft/server/level/ServerLevel";
  28      private static final String CLIENT_LEVEL = "net/minecraft/client/multiplayer/ClientLevel";
  29      private static final String MINECRAFT = "net/minecraft/client/Minecraft";
  30      private static final String KEY_MAPPING = "net/minecraft/client/KeyMapping";
  31      private static final String MOUSE_HANDLER = "net/minecraft/client/MouseHandler";
  32      private static final String KEYBOARD_HANDLER = "net/minecraft/client/KeyboardHandler";
  33      private static final String SCREEN = "net/minecraft/client/gui/screens/Screen";
  34      private static final String OVERLAY = "net/minecraft/client/gui/screens/Overlay";
  35      private static final String HOOKS = "dev/ronova/coremod/agent/BootstrapHooks";
  36      private static final Set<String> REMOVE_NAMES = Set.of("remove", "m_142687_");
  37      private static final Set<String> REVIVE_NAMES = Set.of("revive");
  38      private static final Set<String> HURT_NAMES = Set.of("hurt", "m_6469_");
  39      private static final Set<String> DIE_NAMES = Set.of("die", "m_6667_");
  40      private static final Set<String> HEALTH_NAMES = Set.of("setHealth", "m_21153_");
  41      private static final Set<String> REMOVAL_FIELD_NAMES = Set.of("removalReason", "f_146795_");
  42      private static final Set<String> ADDED_FIELD_NAMES = Set.of("isAddedToWorld");
  43      private static final Set<String> DEAD_FIELD_NAMES = Set.of("dead", "f_20890_");
  44      private static final Set<String> DEATH_TIME_FIELD_NAMES = Set.of("deathTime", "f_20919_");
  45      private static final Set<String> RENDER_NAMES = Set.of("render", "m_7392_");
  46      private static final Set<String> QUERY_COLLECTION_TYPES = Set.of(
  47          "java/lang/Iterable", "java/util/Collection", "java/util/List", "java/util/Set"
  48      );
  49      private static final Set<String> SCREEN_SINK_NAMES = Set.of(
  50          "setScreen", "m_91152_", "forceSetScreen", "m_91346_",
  51          "setOverlay", "m_91150_", "clearLevel", "m_91320_", "m_91399_"
  52      );
  53      private static final Set<String> KEY_SINK_NAMES = Set.of("releaseAll", "m_90847_");
  54      private static final Set<String> MOUSE_SINK_NAMES = Set.of("releaseMouse", "m_91602_");
  55      @SuppressWarnings("unused")
  56      private final String phase;
  57  
  58      RonovaTransformer(String phase) {
  59          this.phase = phase;
  60      }
  61  
  62      static boolean isTarget(String internalName) {
  63          return EntityDataAccessorContract.OWNER.equals(internalName) || NbtFactoryContract.OWNER.equals(internalName) || NbtCopyContract.OWNER.equals(internalName) || RawAccessorContract.registered(internalName) || ENTITY.equals(internalName) || LIVING.equals(internalName)
  64              || LEVEL.equals(internalName) || SERVER_LEVEL.equals(internalName)
  65              || CLIENT_LEVEL.equals(internalName);
  66      }
  67  
  68      static boolean shouldInspect(String internalName, byte[] classBytes) {
  69          if(JdkTaskBoundaryContract.enabled()&&JdkTaskBoundaryContract.registered(internalName))return classBytes!=null;
  70          if (internalName == null || classBytes == null || internalName.startsWith("java/")
  71              || internalName.startsWith("jdk/") || internalName.startsWith("sun/")) {
  72              return false;
  73          }
  74          return isTarget(internalName)
  75              || hasEntityReference(classBytes)
  76              || (containsAscii(classBytes, "net/minecraft/client/renderer/entity/")
  77                  && (containsAscii(classBytes, "render") || containsAscii(classBytes, "m_7392_")))
  78              || hasClientControlReference(classBytes)
  79              || containsAscii(classBytes, "f_146795_")
  80              || containsAscii(classBytes, "isAddedToWorld")
  81              || containsAscii(classBytes, "f_20890_")
  82              || containsAscii(classBytes, "f_20919_")
  83              || containsAscii(classBytes, "removalReason")
  84              || containsAscii(classBytes, "deathTime");
  85      }
  86  
  87      @Override
  88      public byte[] transform(
  89          Module module,
  90          ClassLoader loader,
  91          String className,
  92          Class<?> classBeingRedefined,
  93          ProtectionDomain protectionDomain,
  94          byte[] classfileBuffer
  95      ) {
  96          if (!shouldInspect(className, classfileBuffer)) {
  97              return null;
  98          }
  99          if(JdkTaskBoundaryContract.registered(className))return JdkTaskBoundaryContract.transform(className,classfileBuffer);
 100          try {
 101              ClassNode node = new ClassNode();
 102              // Preserve ModLauncher's frames. Recomputing every frame in Entity during
 103              // premain is unsafe because its transforming module hierarchy is not yet
 104              // visible to the system class loader.
 105              new ClassReader(classfileBuffer).accept(node, 0);
 106              boolean changed = false;
 107              changed |= RawAccessorContract.repair(node);
 108              changed |= EntityDataAccessorContract.repair(node);
 109              changed |= NbtCopyContract.patch(node);
 110              changed |= NbtFactoryContract.patch(node);
 111              if (ENTITY.equals(className)) {
 112                  changed |= patchEntity(node);
 113              } else if (LIVING.equals(className)) {
 114                  changed |= patchLiving(node);
 115              }
 116              if (LEVEL.equals(className) || SERVER_LEVEL.equals(className) || CLIENT_LEVEL.equals(className)) {
 117                  changed |= patchQueryReturns(node);
 118              }
 119              if (!className.startsWith("net/minecraft/")
 120                  && !className.startsWith("dev/ronova/coremod/")) {
 121                  changed |= patchExternalFieldWrites(node);
 122                  changed |= patchExternalQueryCollections(node);
 123                  changed |= patchExternalControlEntrypoints(node);
 124                  changed |= patchExternalControlGuards(node);
 125                  changed |= patchExternalRendererEntries(node);
 126              }
```

<a id="native-negotiation-existing"></a>
### `src/main/java/dev/ronova/coremod/nativebridge/NativeBridge.java` · 31–80

```text
  31      public static synchronized State load(Path gameDirectory) {
  32          if (state != State.NOT_ATTEMPTED) {
  33              return state;
  34          }
  35          String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  36          String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
  37          if (!os.contains("win") || !(architecture.equals("amd64") || architecture.equals("x86_64"))) {
  38              return state = State.UNSUPPORTED_PLATFORM;
  39          }
  40  
  41          try {
  42              System.load(extract(gameDirectory, RESOURCE, "ronova").toString());
  43              int integrityCapabilities = bootstrap();
  44              try {
  45                  taskHooks=prepareTaskBridge0(extract(gameDirectory,"/META-INF/ronova/task-hook-support.jar","ronova-task-hooks",".jar").toString());
  46                  if(taskHooks!=null){dev.ronova.coremod.agent.JdkTaskBoundaryContract.enable();refreshTaskObservers0();}
  47              } catch(Throwable unavailable){taskHooks=null;System.err.println("RONOVA_TASK_BRIDGE_UNAVAILABLE "+unavailable);}
  48              try (InputStream sealId=NativeBridge.class.getResourceAsStream("/META-INF/ronova/code-seal-id.txt")) {
  49                  if(sealId!=null){String id=new String(sealId.readNBytes(65),java.nio.charset.StandardCharsets.US_ASCII);
  50                      if(id.matches("[0-9a-f]{64}"))enableCodeSeals0(id);}
  51              }
  52              int rootCapabilities = 0;
  53              try {
  54                  System.load(extract(
  55                      gameDirectory, ROOT_RESOURCE, "ronova_root"
  56                  ).toString());
  57                  rootCapabilities = NativeRootBridge.bootstrapLibrary();
  58              } catch (Throwable ignored) {
  59                  // Integrity remains available while /ronova status reports Java fallback.
  60              }
  61              capabilities = integrityCapabilities | rootCapabilities;
  62              state = (integrityCapabilities & 0x7) == 0x7
  63                  ? State.ACTIVE : State.JVMTI_LIMITED;
  64              Runtime.getRuntime().addShutdownHook(new Thread(() -> {
  65                  watchdogRequested=false;
  66                  try {
  67                      NativeRootBridge.shutdownLibrary();
  68                  } catch (Throwable ignored) {
  69                      // Root DLL is optional on unsupported or degraded installations.
  70                  }
  71                  try {
  72                      shutdown();
  73                  } catch (Throwable ignored) {
  74                      // The operating system will reclaim the library at process exit.
  75                  }
  76              }, "Ronova-Native-Shutdown"));
  77          } catch (Throwable ignored) {
  78              state = State.LOAD_FAILED;
  79          }
  80          return state;
```

<a id="native-mask"></a>
### `src/main/java/dev/ronova/coremod/nativebridge/NativeBridge.java` · 115–121

```text
 115      public static State state() {
 116          return state;
 117      }
 118  
 119      public static int capabilityMask() {
 120          return capabilities;
 121      }
```

<a id="retry-queue-contract"></a>
### `src/main/java/dev/ronova/coremod/engine/spawn/BoundedRetryQueue.java` · 1–48

```text
   1  package dev.ronova.coremod.engine.spawn;
   2  
   3  import dev.ronova.coremod.budget.BudgetReceipt;
   4  import dev.ronova.coremod.budget.WorkBudget;
   5  import java.util.ArrayDeque;
   6  import java.util.List;
   7  import java.util.Objects;
   8  import java.util.function.Predicate;
   9  
  10  /** Thread-safe FIFO drain with finite per-entry retries and bounded dead letters. */
  11  final class BoundedRetryQueue<T> {
  12      static final int DEFAULT_MAX_ATTEMPTS = 8;
  13      static final int DEFAULT_DEAD_LETTER_CAPACITY = 1_024;
  14  
  15      private final Object queueLock = new Object();
  16      private final Object drainLock = new Object();
  17      private final ArrayDeque<Entry<T>> queue = new ArrayDeque<>();
  18      private final ArrayDeque<DeadLetter<T>> deadLetters = new ArrayDeque<>();
  19      private final int maxAttempts;
  20      private final int deadLetterCapacity;
  21      private final java.util.function.Consumer<T> dispose;
  22      private long deadLetteredTotal;
  23  
  24      BoundedRetryQueue() {
  25          this(DEFAULT_MAX_ATTEMPTS, DEFAULT_DEAD_LETTER_CAPACITY);
  26      }
  27  
  28      BoundedRetryQueue(int maxAttempts, int deadLetterCapacity) {
  29          this(maxAttempts, deadLetterCapacity, ignored -> {});
  30      }
  31  
  32      BoundedRetryQueue(int maxAttempts, int deadLetterCapacity, java.util.function.Consumer<T> dispose) {
  33          if (maxAttempts <= 0) {
  34              throw new IllegalArgumentException("maxAttempts must be positive");
  35          }
  36          if (deadLetterCapacity <= 0) {
  37              throw new IllegalArgumentException("deadLetterCapacity must be positive");
  38          }
  39          this.maxAttempts = maxAttempts;
  40          this.deadLetterCapacity = deadLetterCapacity;
  41          this.dispose = Objects.requireNonNull(dispose, "dispose");
  42      }
  43  
  44      void addLast(T value) {
  45          addLast(new Entry<>(Objects.requireNonNull(value, "value"), 0));
  46      }
  47  
  48      /** Runs one ungated snapshot; a failed head cannot starve later entries. */
```

<a id="retry-false-vs-exception"></a>
### `src/main/java/dev/ronova/coremod/engine/spawn/BoundedRetryQueue.java` · 165–193

```text
 165      /** Returns true when the entry remains active, false when it is quarantined. */
 166      private boolean retryOrDeadLetter(Entry<T> entry) {
 167          int attempts = entry.failedAttempts() + 1;
 168          if (attempts < maxAttempts) {
 169              addLast(new Entry<>(entry.value(), attempts));
 170              return true;
 171          }
 172          synchronized (queueLock) {
 173              if (deadLetters.size() >= deadLetterCapacity) {
 174                  dispose.accept(deadLetters.removeFirst().value());
 175              }
 176              deadLetters.addLast(new DeadLetter<>(entry.value(), attempts));
 177              if (deadLetteredTotal != Long.MAX_VALUE) {
 178                  deadLetteredTotal++;
 179              }
 180          }
 181          return false;
 182      }
 183  
 184      private void addLast(Entry<T> entry) {
 185          synchronized (queueLock) {
 186              queue.addLast(entry);
 187          }
 188      }
 189  
 190      private boolean process(Entry<T> entry, Processor<? super T> processor) {
 191          try { return processor.process(entry.value()); }
 192          catch (RuntimeException | Error failed) { addLast(entry); throw failed; }
 193      }
```

<a id="root-fence"></a>
### `src/main/java/dev/ronova/coremod/root/api/RootExecutionFence.java` · 1–73

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Objects;
   4  import java.util.function.LongSupplier;
   5  
   6  /** Excludes new canonical commits during one registered raw-write step of this Root instance. */
   7  public final class RootExecutionFence {
   8      public static final long MAX_DURATION_NANOS=250_000_000L;
   9      private final LongSupplier clock;
  10      private Lease active;
  11      public RootExecutionFence() { this(System::nanoTime); }
  12      RootExecutionFence(LongSupplier clock) { this.clock=Objects.requireNonNull(clock); }
  13      public synchronized Lease tryAcquire(TerminalOperationPlan plan) {
  14          Objects.requireNonNull(plan);
  15          if(active!=null)return null;
  16          active=new Lease(plan,clock.getAsLong());return active;
  17      }
  18      public synchronized Lease tryAcquireOwner(AuthorityStamp authority) {
  19          Objects.requireNonNull(authority);
  20          if(active!=null||!authority.owner()||!authority.settled())return null;
  21          active=new Lease(authority,clock.getAsLong());return active;
  22      }
  23      public synchronized boolean busy() { return active!=null; }
  24      public synchronized boolean ownsStorage(Lease lease,RecoveryStoragePlan plan) {
  25          return lease!=null&&active==lease&&lease.valid(plan);
  26      }
  27      public synchronized Lease tryAcquireRecord(RecoveryRecordPlan plan) {
  28          Objects.requireNonNull(plan);
  29          if(active!=null)return null;
  30          active=new Lease(plan,clock.getAsLong());return active;
  31      }
  32      public synchronized Lease tryAcquireSnapshot(RecoverySnapshotPlan plan) {
  33          Objects.requireNonNull(plan);
  34          if(active!=null)return null;
  35          active=new Lease(plan,clock.getAsLong());return active;
  36      }
  37      public synchronized Lease tryAcquireTask(RecoveryTaskPlan plan) {
  38          Objects.requireNonNull(plan);if(active!=null)return null;active=new Lease(plan,clock.getAsLong());return active;
  39      }
  40      public synchronized Lease tryAcquireStorage(RecoveryStoragePlan plan) {
  41          Objects.requireNonNull(plan);if(active!=null)return null;active=new Lease(plan,clock.getAsLong());return active;
  42      }
  43  
  44      public final class Lease implements AutoCloseable {
  45          private final Object scope;
  46          private final long issued;
  47          private final Thread owner=Thread.currentThread();
  48          private boolean revoked;
  49          private Lease(Object scope,long issued) { this.scope=scope;this.issued=issued; }
  50          public boolean valid(TerminalOperationPlan expected) {return validScope(expected);}
  51          public boolean valid(AuthorityStamp expected) {return validScope(expected);}
  52          public boolean valid(RecoverySnapshotPlan expected) {return validScope(expected);}
  53          public boolean valid(RecoveryRecordPlan expected) {return validScope(expected);}
  54          public boolean valid(RecoveryTaskPlan expected) {return validScope(expected);}
  55          public boolean valid(RecoveryStoragePlan expected) {return validScope(expected);}
  56          private boolean validScope(Object expected) {
  57              synchronized(RootExecutionFence.this) {
  58                  long age=clock.getAsLong()-issued;
  59                  if(active!=this||revoked)return false;
  60                  if(Thread.currentThread()!=owner||!scope.equals(expected)||age<0||age>=MAX_DURATION_NANOS){revoked=true;return false;}
  61                  return true;
  62              }
  63          }
  64          @Override public void close() {
  65              synchronized(RootExecutionFence.this) {
  66                  if(Thread.currentThread()!=owner)throw new IllegalStateException("Root execution fence belongs to another thread");
  67                  if(active==this)active=null;
  68              }
  69          }
  70      }
  71      // Expiry revokes further writes but never forcibly releases a possibly paused
  72      // writer. The owning try/finally must leave the step before a new commit enters.
  73  }
```
