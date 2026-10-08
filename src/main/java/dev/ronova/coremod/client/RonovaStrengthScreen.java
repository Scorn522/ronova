package dev.ronova.coremod.client;

import dev.ronova.coremod.network.StrengthControl;
import dev.ronova.coremod.policy.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Five player-facing pages backed by versioned server policy receipts. */
public final class RonovaStrengthScreen extends Screen {
    private static final String[] PAGES={"攻击","拦截","防护","运行时","功能"};
    private static final String[] OPTIONS={
        "非实体攻击","逻辑源攻击","恢复源攻击","状态源攻击","注册源攻击","任务链攻击","回调链攻击","网络重建攻击","存档重建攻击","动态代码攻击","分派攻击","运行时补丁失能","目标类血缘压制","最终结果接管","攻击来源追踪",
        "Mixin","ASM / Javassist","Instrumentation","JVMTI","Unsafe / 反射","ClassLoader","JNI / Native","ModLauncher","线程与任务","全返回拦截",
        "生命保护","死亡保护","清除保护","实体记录保护","身份与重生保护","显示保护","反拦截","反全返回","按键与通信保护","调用保护","代码防篡改","核心权限保护","自动修复","状态恢复","重启恢复","防状态回滚","死循环保护",
        "Java Agent","Instrumentation","JVMTI","Native","本地库加载","渲染保护","关键类监视","动态代码监视","Native 模块监视","线程连续性","后台完整性检查","异常调用记录"};
    static { if(OPTIONS.length!=StrengthOptionKey.values().length)throw new ExceptionInInitializerError("incomplete strength labels"); }
    private final Screen parent;
    private int page,offset;
    private boolean advanced;
    private UUID shownRequest;
    private boolean shownPending;
    private boolean automaticRead;
    private String status="等待服务端状态";
    private final List<Row> visible=new ArrayList<>();
    private record Row(String label,String state,int y) {}
    public RonovaStrengthScreen(Screen parent){super(Component.literal("若娜瓦 · 强度设置"));this.parent=parent;}
    public void selectPage(int selected){if(selected<0||selected>=PAGES.length)throw new IllegalArgumentException("invalid page");page=selected;offset=0;rebuildWidgets();}
    private int rows(){return Math.max(1,(height-184)/24);}
    private int completionDetailY(){return page==4?Math.min(height-40,Math.max(172,height-68)):height-68;}
    private Button button(String text,int x,int y,int w,Runnable run,boolean active) {
        var b=addRenderableWidget(Button.builder(Component.literal(text),ignored->run.run()).bounds(x,y,w,20).build());b.active=active;
        String hint = switch (text) {
            case "兼容" -> "基础清除与旧资格拒绝；恢复链保留基础观察，不主动深挖第三方容器。";
            case "强化" -> "识别目标的直接恢复记录、快照、任务和重建入口；实际处置取决于对应能力与当前证据。";
            case "极限代码对抗", "极限" -> "继续追踪派生快照、后继任务、换身份与保存重建链。来源未知或效果未确认时仍显示未完成。";
            default -> null;
        };
        if(hint!=null)b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(hint)));
        return b;
    }
    @Override protected void init() {
        visible.clear();
        var state=ClientRuntime.strengthControls();var snapshot=state.snapshot();
        if(snapshot==null&&!state.pending())ClientRuntime.requestStrength(StrengthControl.Operation.READ,0,0);
        int tabWidth=(width-32)/5;
        for(int i=0;i<5;i++){int tab=i;button(PAGES[i],16+i*tabWidth,38,tabWidth-3,()->selectPage(tab),page!=i);}
        boolean editable=snapshot!=null&&snapshot.editable()&&state.fresh(System.nanoTime()/1_000_000L)&&!state.pending();
        if(page!=4) {
            int presetWidth=(width-32)/4;
            for(var profile:StrengthProfile.values())button(profile(profile),16+profile.ordinal()*presetWidth,66,presetWidth-3,
                ()->send(StrengthControl.Operation.PROFILE,0,profile.ordinal()),editable);
        }
        if(page==4) {
            button("客户端显示与启动设置…",16,102,Math.min(width-32,240),()->minecraft.setScreen(new RonovaControlScreen(this)),true);
            status="游戏功能独立保存，强度预设不会修改这些设置";
        } else if(snapshot!=null) {
            var keys=new ArrayList<StrengthOptionKey>();
            for(var key:StrengthOptionKey.values())if(key.plane().ordinal()==page)keys.add(key);
            boolean actions=page==0&&!advanced;
            int count=actions?AttackAction.values().length+1:keys.size();
            offset=Math.min(offset,Math.max(0,((count-1)/rows())*rows()));
            for(int n=offset;n<Math.min(count,offset+rows());n++) {
                int y=116+(n-offset)*24;int controlX=Math.max(132,width/2-10);int controlWidth=Math.min(112,(width-32)/3);
                if(actions&&n<AttackAction.values().length) {
                    var key=AttackAction.values()[n];var value=snapshot.actions().get(n);
                    visible.add(new Row(key.displayName(),"当前："+actionCapability(value),y));
                    button(action(value.configured()),controlX,y,controlWidth,()->send(StrengthControl.Operation.ACTION,key.ordinal(),
                        (value.configured().ordinal()+1)%AttackActionLevel.values().length),editable)
                        .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(actionDetail(value,snapshot))));
                } else {
                    var key=actions?StrengthOptionKey.ATTACK_SOURCE_TRACE:keys.get(n);var value=snapshot.options().get(key.ordinal());
                    visible.add(new Row(OPTIONS[key.ordinal()],value.activation()==ActivationState.ACTIVE?"生效："+level(value.effective()):activation(value.activation()),y));
                    button(level(value.configured()),controlX,y,controlWidth,()->send(StrengthControl.Operation.OPTION,key.ordinal(),
                        (value.configured().ordinal()+1)%MechanismLevel.values().length),editable)
                        .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(optionDetail(value))));
                }
            }
            if(page==0)button(advanced?"普通攻击":"高级攻击",16,height-58,86,()->{advanced=!advanced;offset=0;rebuildWidgets();},true);
            button("上一页",width-150,height-58,62,()->{offset=Math.max(0,offset-rows());rebuildWidgets();},offset>0);
            button("下一页",width-82,height-58,66,()->{offset+=rows();rebuildWidgets();},offset+rows()<count);
        }
        button("刷新",16,height-28,62,()->send(StrengthControl.Operation.READ,0,0),!state.pending());
        button("完成",width-82,height-28,66,this::onClose,true);
        shownPending=state.pending();shownRequest=snapshot==null?null:snapshot.requestId();
    }
    private void send(StrengthControl.Operation operation,int key,int value) {
        automaticRead=false;
        status=ClientRuntime.requestStrength(operation,key,value)?"等待服务端确认":"当前无法提交，稍后刷新";rebuildWidgets();
    }
    @Override public void tick() {
        long now=System.nanoTime()/1_000_000L;
        var state=ClientRuntime.strengthControls();state.expire(now);var snapshot=state.snapshot();
        if(snapshot==null&&shownRequest!=null){status="连接已变化，等待当前服务端状态";automaticRead=false;rebuildWidgets();}
        else if(snapshot!=null&&!Objects.equals(shownRequest,snapshot.requestId())) {
            if(!automaticRead||snapshot.outcome()!=StrengthControl.Outcome.SNAPSHOT)status=switch(snapshot.outcome()) {
                case SAVED -> snapshot.pendingRestart()?"已保存；部分设置等待重启":"服务端已保存";
                case STALE -> "配置已变化，已刷新；重新选择后再提交";
                case FORBIDDEN -> "只有服务端管理员可以修改";
                case UNAVAILABLE -> "服务端尚未确认修改，刷新后核对";
                case SNAPSHOT -> snapshot.editable()?"已读取服务端状态":"只读：需要服务端管理员权限";
            };
            if(automaticRead&&snapshot.outcome()==StrengthControl.Outcome.SNAPSHOT)status=snapshot.editable()?"服务端状态每秒更新":"只读：服务端状态每秒更新";
            automaticRead=false;rebuildWidgets();
        }else if(shownPending!=state.pending()) {status="请求超时，正在重读服务端状态";automaticRead=false;rebuildWidgets();}
        if(state.refreshDue(now)&&ClientRuntime.requestStrength(StrengthControl.Operation.READ,0,0)){
            automaticRead=true;rebuildWidgets();
        }
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        g.fillGradient(0,0,width,height,0xFF101822,0xFF1D2937);
        g.drawString(font,title,16,16,0xFFEAE2FF,false);
        var controls=ClientRuntime.strengthControls();var snapshot=controls.snapshot();
        boolean fresh=controls.fresh(System.nanoTime()/1_000_000L);
        if(page!=4) {
            String summary=snapshot==null?"未连接到已就绪的服务端": "当前选择："+profile(snapshot.configured())+"  当前状态："+(fresh?effectiveState(snapshot):"状态已过期，正在刷新");
            g.drawString(font,font.plainSubstrByWidth(summary,width-32),16,94,0xFFA8CBD4,false);
            for(var row:visible) {
                g.drawString(font,font.plainSubstrByWidth(row.label(),Math.max(104,width/2-38)),16,row.y()+6,0xFFFFFFFF,false);
                int x=Math.max(132,width/2-10)+Math.min(112,(width-32)/3)+8;
                if(width-x>20)g.drawString(font,font.plainSubstrByWidth(fresh?row.state():"快照已过期",width-x-16),x,row.y()+6,0xFF99B7BC,false);
            }
            if(snapshot!=null) {
                String result=switch(snapshot.attackOutcome()){case NO_OPERATION->"暂无操作";case TERMINAL_COMMITTED->"已裁决";case CHANGED->"判决已变化";};
                String execution=switch(snapshot.execution()){case IDLE->"未执行";case PARTIAL->"部分执行";case FENCED->"已隔离";};
                String operation=fresh?"服务端快照："+result+" · "+(snapshot.completionSatisfied()?"已完成":"未确认完成")+" · "+execution:"服务端快照已过期，当前结果待重新确认";
                g.drawString(font,font.plainSubstrByWidth(operation,width-32),16,height-80,0xFFADBDC8,false);
                g.drawString(font,font.plainSubstrByWidth(fresh?snapshot.completionDetail():"上次观察已过期；等待服务端重新确认",width-32),16,height-68,0xFFADBDC8,false);
            }
        }
        if(page==4) {
            int contentY=height>=216?148:124;
            if(height>=216)g.drawString(font,font.plainSubstrByWidth("显示与普通游戏功能不会随强度预设变化",width-32),16,136,0xFF99B7BC,false);
            var memory=ClientRuntime.projectionMemoryUsage();
            String usage="客户端投影："+ClientRuntime.projectionMarkCount()+" 条 · 保留 "+(memory.retainedBytes()/1024)+" / "+(memory.budgetBytes()/1024)+" KiB";
            g.drawString(font,font.plainSubstrByWidth(usage,width-32),16,contentY,0xFF99B7BC,false);
            if(contentY+24<=completionDetailY())g.drawString(font,font.plainSubstrByWidth("预算拒绝 "+memory.denials()+" 次；回执确认数据，不证明第三方画面已清除。",width-32),16,contentY+12,0xFF99B7BC,false);
            if(snapshot!=null)g.drawString(font,font.plainSubstrByWidth(fresh?snapshot.completionDetail():"服务端状态已过期，正在刷新",width-32),16,completionDetailY(),0xFFADBDC8,false);
        }
        g.drawString(font,font.plainSubstrByWidth(status,Math.max(40,width-180)),86,height-22,0xFFD3BDF8,false);
        super.render(g,mouseX,mouseY,partialTick);
        if(snapshot!=null&&mouseX>=16&&mouseX<width-16&&mouseY>=completionDetailY()-12&&mouseY<completionDetailY()+10
            &&!snapshot.completionDetail().isEmpty())g.renderTooltip(font,Component.literal((fresh?"":"历史快照（已过期）：")+snapshot.completionDetail()),mouseX,mouseY);
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    private static String optionDetail(StrengthControl.Option option) {
        return OPTIONS[option.key().ordinal()]+"\n配置："+level(option.configured())+"\n当前能力："+level(option.effective())
            +"\n状态："+activation(option.activation())+"\n能力状态表示服务端登记路径的可用范围，不代表本次攻击已完成。";
    }
    private static String actionDetail(StrengthControl.Action action,StrengthControl.Snapshot snapshot) {
        var text=new StringBuilder(action.key().displayName()).append("\n配置：").append(action(action.configured()))
            .append("\n当前能力：").append(actionCapability(action));
        if(action.effective()==MechanismLevel.PROTECT_RONOVA) {
            switch(action.key()) {
                case REWRITE_LIFE->text.append("\n范围：已注册实体的当前血量字段；其他生命来源单独判断。");
                case DIRECT_CLEAR->text.append("\n范围：已注册实体的移除字段；不表示所有索引和恢复源已清除。");
                case CLEAR_ENTITY_RECORDS->text.append("\n范围：已移除实体的已注册索引；未移除目标还要求直接清除。");
                default->{ }
            }
        }
        if(snapshot.pendingRestart())text.append("\n部分配置等待重启；当前值使用活动配置。");
        action.key().requiredKeys().stream().sorted(Comparator.comparingInt(StrengthOptionKey::ordinal)).forEach(key->{
            var dependency=snapshot.options().get(key.ordinal());
            text.append("\n高级项整体范围 ").append(OPTIONS[key.ordinal()]).append("：").append(level(dependency.effective()))
                .append(" / ").append(activation(dependency.activation()));
        });
        return text.append("\n完成状态以服务端观察为准；客户端回执仅确认投影数据。").toString();
    }
    private static String actionCapability(StrengthControl.Action action) {
        if(action.effective()==MechanismLevel.PROTECT_RONOVA&&java.util.Set.of(AttackAction.REWRITE_LIFE,
            AttackAction.DIRECT_CLEAR,AttackAction.CLEAR_ENTITY_RECORDS).contains(action.key()))return "实体局部路径";
        return level(action.effective());
    }
    private static String profile(StrengthProfile p){return switch(p){case COMPATIBLE->"兼容";case ENHANCED->"强化";case EXTREME->"极限代码对抗";case CUSTOM->"自定义";};}
    private static String action(AttackActionLevel l){return switch(l){case FOLLOW_PROFILE->"跟随预设";case DISABLED->"关闭";case COMPATIBLE->"兼容";case ENHANCED->"强化";case EXTREME->"极限";};}
    private static String level(MechanismLevel l){return switch(l){case FOLLOW_PROFILE->"跟随预设";case OFF->"关闭";case MONITOR->"观察";case PROTECT_RONOVA->"基础保护";case RELATED_PATHS->"相关路径";case STRICT_ALL->"全部严格";};}
    private static String activation(ActivationState s){return switch(s){case INACTIVE->"未启用";case ACTIVE->"已生效";case PENDING_RESTART->"等待重启";case UNAVAILABLE->"能力不足";case REJECTED->"请求被拒绝";case FENCED->"已隔离";};}
    private static String effectiveState(StrengthControl.Snapshot s) {
        if(s.pendingRestart())return "等待重启";
        if(s.options().stream().anyMatch(o->o.activation()==ActivationState.FENCED))return "部分隔离";
        if(s.options().stream().anyMatch(o->o.activation()==ActivationState.UNAVAILABLE||o.activation()==ActivationState.REJECTED))
            return s.options().stream().anyMatch(o->o.effective()!=MechanismLevel.OFF)?"部分生效":"能力不足";
        return "已生效";
    }
}
