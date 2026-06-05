package com.honmen.drivingexam.subject3;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.honmen.drivingexam.subject3.ActionType.ALTERNATE_HIGH_LOW_BEAM;
import static com.honmen.drivingexam.subject3.ActionType.BRAKE;
import static com.honmen.drivingexam.subject3.ActionType.CHANGE_LANE;
import static com.honmen.drivingexam.subject3.ActionType.CHECK_MIRROR;
import static com.honmen.drivingexam.subject3.ActionType.CLUTCH;
import static com.honmen.drivingexam.subject3.ActionType.FASTEN_SEAT_BELT;
import static com.honmen.drivingexam.subject3.ActionType.FOG_LIGHT;
import static com.honmen.drivingexam.subject3.ActionType.HAZARD_LIGHT;
import static com.honmen.drivingexam.subject3.ActionType.HIGH_BEAM;
import static com.honmen.drivingexam.subject3.ActionType.HONK;
import static com.honmen.drivingexam.subject3.ActionType.KEEP_STRAIGHT;
import static com.honmen.drivingexam.subject3.ActionType.LOOK_BACK;
import static com.honmen.drivingexam.subject3.ActionType.LOW_BEAM;
import static com.honmen.drivingexam.subject3.ActionType.OBSERVE_LEFT_RIGHT;
import static com.honmen.drivingexam.subject3.ActionType.PULL_OVER;
import static com.honmen.drivingexam.subject3.ActionType.RELEASE_HANDBRAKE;
import static com.honmen.drivingexam.subject3.ActionType.SHIFT_GEAR;
import static com.honmen.drivingexam.subject3.ActionType.SLOW_DOWN;
import static com.honmen.drivingexam.subject3.ActionType.START_ENGINE;
import static com.honmen.drivingexam.subject3.ActionType.STOP;
import static com.honmen.drivingexam.subject3.ActionType.TURN_SIGNAL_LEFT;
import static com.honmen.drivingexam.subject3.ActionType.TURN_SIGNAL_OFF;
import static com.honmen.drivingexam.subject3.ActionType.TURN_SIGNAL_RIGHT;
import static com.honmen.drivingexam.subject3.ActionType.U_TURN;
import static com.honmen.drivingexam.subject3.RoadScene.bus_station;
import static com.honmen.drivingexam.subject3.RoadScene.crosswalk;
import static com.honmen.drivingexam.subject3.RoadScene.intersection;
import static com.honmen.drivingexam.subject3.RoadScene.lane_change;
import static com.honmen.drivingexam.subject3.RoadScene.night_light;
import static com.honmen.drivingexam.subject3.RoadScene.normal_road;
import static com.honmen.drivingexam.subject3.RoadScene.overtaking;
import static com.honmen.drivingexam.subject3.RoadScene.pull_over;
import static com.honmen.drivingexam.subject3.RoadScene.school_zone;
import static com.honmen.drivingexam.subject3.RoadScene.u_turn;
import static com.honmen.drivingexam.subject3.VehicleType.automatic;
import static com.honmen.drivingexam.subject3.VehicleType.manual;

public final class Subject3ProjectConfig {
    private static final List<VehicleType> ALL_VEHICLES = List.of(manual, automatic);

    private Subject3ProjectConfig() {
    }

    public static List<ExamProject> defaultProjects() {
        return List.of(
            boardingPreparation(),
            startDriving(),
            straightDriving(),
            gearOperation(),
            laneChange(),
            pullOverProject(),
            passIntersection("intersection_straight", "直行通过路口", "按导向车道和信号灯直行通过路口。"),
            passIntersection("intersection_left", "路口左转弯", "进入路口前减速观察，按规定路线左转。"),
            passIntersection("intersection_right", "路口右转弯", "进入路口前减速观察，确认安全后右转。"),
            passSlowArea("crosswalk", "通过人行横道线", crosswalk),
            passSlowArea("school_zone", "通过学校区域", school_zone),
            passSlowArea("bus_station", "通过公共汽车站", bus_station),
            meetingTraffic(),
            overtakingProject(),
            uTurnProject(),
            nightLightProject()
        );
    }

    private static ExamProject boardingPreparation() {
        String id = "boarding_preparation";
        return project(id, "上车准备", "完成绕车检查、观察后方、系安全带和调整后视镜。", List.of(normal_road),
            steps(
                step("walk_around", OBSERVE_LEFT_RIGHT, "绕车检查", "检查车辆周围和车身状态。", 1, 20),
                step("door_observe", LOOK_BACK, "打开车门前观察后方", "开门前观察后方交通情况。", 2, 8),
                step("seat_belt", FASTEN_SEAT_BELT, "系安全带", "上车后立即系好安全带。", 3, 10),
                step("adjust_seat", CHECK_MIRROR, "调整座椅", "调整到合适驾驶坐姿。", 4, 12),
                step("adjust_mirror", CHECK_MIRROR, "调整后视镜", "确认内外后视镜视野。", 5, 12)
            ),
            List.of(),
            failRules(id,
                rule(id, "missing_walk", "MISSING_STEP:walk_around", 100, true, "未绕车检查，不合格"),
                rule(id, "missing_door_observe", "MISSING_STEP:door_observe", 100, true, "打开车门前未观察后方，不合格"),
                rule(id, "missing_seat_belt", "MISSING_STEP:seat_belt", 100, true, "未系安全带，不合格")
            ),
            tips("上车前先观察车辆周围，开门前回头看，安全带不要忘。")
        );
    }

    private static ExamProject startDriving() {
        String id = "start";
        return project(id, "起步", "检查车辆状态，观察确认安全后平稳起步。", List.of(normal_road),
            steps(
                step("door_closed", OBSERVE_LEFT_RIGHT, "检查车门是否关闭", "确认车门关闭、仪表无异常。", 1, 8),
                step("seat_belt", FASTEN_SEAT_BELT, "系安全带", "确认安全带已系好。", 2, 8),
                step("gear_check", SHIFT_GEAR, "检查挡位", "手动挡为空挡，自动挡为 P 或 N。", 3, 10),
                step("engine_start", START_ENGINE, "启动发动机", "挡位正确后启动发动机。", 4, 8),
                step("mirror_observe", CHECK_MIRROR, "观察后视镜", "观察内外后视镜。", 5, 8),
                step("look_back", LOOK_BACK, "回头观察", "回头观察后方交通情况。", 6, 8),
                step("left_signal", TURN_SIGNAL_LEFT, "开启左转向灯", "起步前开启左转向灯。", 7, 8),
                step("release_handbrake", RELEASE_HANDBRAKE, "松驻车制动", "确认安全后松开驻车制动。", 8, 8),
                step("manual_clutch", CLUTCH, "控制离合", "手动挡平稳抬离合。", 9, 8, List.of(manual), List.of(normal_road)),
                step("smooth_start", BRAKE, "平稳起步", "平稳起步，不闯动不熄火。", 10, 12)
            ),
            scoreRules(id,
                rule(id, "stall", "CONTEXT_TRUE:engineStalled", 10, false, "起步熄火，扣 10 分"),
                rule(id, "handbrake_not_released", "CONTEXT_TRUE:handbrakeNotReleased", 10, false, "未松驻车制动起步且未及时纠正，扣分")
            ),
            failRules(id,
                rule(id, "door_open", "CONTEXT_FALSE:doorClosed", 100, true, "车门未关闭起步，不合格"),
                rule(id, "missing_observe", "MISSING_STEP:mirror_observe", 100, true, "起步前未观察，不合格"),
                rule(id, "wrong_gear", "CONTEXT_TRUE:wrongGearOnStart", 100, true, "启动发动机时挡位错误，不合格")
            ),
            tips("手动挡起步重点是挡位、离合和驻车制动；自动挡重点确认 P/N 挡启动。")
        );
    }

    private static ExamProject straightDriving() {
        String id = "straight_driving";
        return project(id, "直线行驶", "保持车辆沿车道稳定直线行驶。", List.of(normal_road),
            steps(
                step("observe", CHECK_MIRROR, "观察前后交通", "观察前方道路和后视镜。", 1, 8),
                step("keep_straight", KEEP_STRAIGHT, "保持直线", "稳住方向盘，保持车身正直。", 2, 20),
                step("slow_adjust", SLOW_DOWN, "必要时微调速度", "根据道路情况轻微调整速度。", 3, 12)
            ),
            scoreRules(id, rule(id, "direction_unstable", "CONTEXT_TRUE:directionUnstable", 10, false, "方向控制不稳，扣 10 分")),
            failRules(id, rule(id, "lane_departure", "CONTEXT_TRUE:laneDeparture", 100, true, "偏离行驶路线影响安全，不合格")),
            tips("视线放远，方向盘少量微调，避免大幅修正。")
        );
    }

    private static ExamProject gearOperation() {
        String id = "gear_operation";
        return project(id, "加减挡位操作", "根据速度完成平顺加挡、减挡。", List.of(normal_road),
            steps(
                step("observe_speed", CHECK_MIRROR, "观察车速和路况", "确认路况允许后进行挡位操作。", 1, 8),
                step("clutch", CLUTCH, "踩离合", "手动挡换挡前踩下离合。", 2, 6, List.of(manual), List.of(normal_road)),
                step("shift", SHIFT_GEAR, "加减挡位", "根据速度选择合适挡位。", 3, 8),
                step("stabilize", KEEP_STRAIGHT, "保持平稳", "换挡后保持车辆平稳。", 4, 10)
            ),
            scoreRules(id, rule(id, "gear_not_smooth", "CONTEXT_TRUE:gearNotSmooth", 10, false, "换挡不平顺，扣 10 分")),
            failRules(id, rule(id, "wrong_gear_speed", "CONTEXT_TRUE:gearSpeedMismatch", 100, true, "挡位与速度严重不匹配，不合格")),
            tips("自动挡训练保留速度匹配意识；手动挡额外训练离合与挡位配合。")
        );
    }

    private static ExamProject laneChange() {
        String id = "lane_change";
        return project(id, "变更车道", "观察确认安全后平稳变更车道。", List.of(lane_change),
            steps(
                step("signal", TURN_SIGNAL_LEFT, "开启转向灯", "根据目标车道开启对应方向转向灯。", 1, 8),
                step("mirror", CHECK_MIRROR, "观察后视镜", "观察内外后视镜。", 2, 8),
                step("look_back", LOOK_BACK, "回头观察目标车道", "确认目标车道后方交通情况。", 3, 8),
                step("safe_distance", OBSERVE_LEFT_RIGHT, "判断安全距离", "确认不影响其他车辆。", 4, 8),
                step("change_lane", CHANGE_LANE, "平稳变更车道", "平稳驶入目标车道。", 5, 12),
                step("signal_off", TURN_SIGNAL_OFF, "关闭转向灯", "变道完成后关闭转向灯。", 6, 8)
            ),
            scoreRules(id, rule(id, "signal_not_off", "MISSING_STEP:signal_off", 10, false, "操作完成后不关闭转向灯，扣 10 分")),
            failRules(id,
                rule(id, "missing_observe", "MISSING_STEP:mirror", 100, true, "变道前未观察，不合格"),
                rule(id, "unsafe_distance", "CONTEXT_FALSE:safeDistance", 100, true, "安全距离判断不合理，影响其他车辆，不合格")
            ),
            tips("转向灯、镜中观察、回头观察、安全距离，顺序不能乱。")
        );
    }

    private static ExamProject pullOverProject() {
        String id = "pull_over";
        return project(id, "靠边停车", "观察确认安全后靠边停车，控制车身距离。", List.of(pull_over),
            steps(
                step("right_signal", TURN_SIGNAL_RIGHT, "开启右转向灯", "提前开启右转向灯。", 1, 8),
                step("mirror", CHECK_MIRROR, "观察后视镜", "观察内外后视镜。", 2, 8),
                step("look_back", LOOK_BACK, "回头观察右后方", "确认右后方安全。", 3, 8),
                step("slow_down", SLOW_DOWN, "减速靠边", "减速并逐步靠边。", 4, 12),
                step("pull_over", PULL_OVER, "控制 30cm 内停车", "车身距离道路右侧边缘线控制在 30cm 内。", 5, 20),
                step("stop", STOP, "停车并拉紧驻车制动器", "停车后拉紧驻车制动器。", 6, 10),
                step("signal_off", TURN_SIGNAL_OFF, "关闭转向灯", "停车完成后关闭转向灯。", 7, 8)
            ),
            scoreRules(id,
                rule(id, "curb_30_50", "CONTEXT_BETWEEN:curbDistanceCm:30:50", 10, false, "车身距离右侧边缘线 30cm 到 50cm，扣 10 分"),
                rule(id, "parking_brake", "CONTEXT_FALSE:parkingBrakeSet", 10, false, "停车后未拉紧驻车制动器，扣 10 分")
            ),
            failRules(id,
                rule(id, "not_in_distance", "CONTEXT_TRUE:notInRequiredDistance", 100, true, "未在规定距离内停车，不合格"),
                rule(id, "curb_over_50", "CONTEXT_GT:curbDistanceCm:50", 100, true, "车身距离右侧边缘线超过 50cm，不合格")
            ),
            tips("靠边停车先观察再靠边，距离控制是核心。")
        );
    }

    private static ExamProject passIntersection(String id, String name, String description) {
        List<ExamStep> intersectionSteps = id.endsWith("straight")
            ? steps(
                step("slow_down", SLOW_DOWN, "提前减速", "进入路口前提前减速。", 1, 10),
                step("observe", OBSERVE_LEFT_RIGHT, "观察左右交通情况", "观察车辆、行人和非机动车。", 2, 10),
                step("lane", KEEP_STRAIGHT, "按导向车道行驶", "按导向车道和标线行驶。", 3, 12),
                step("yield", BRAKE, "按信号灯或让行规则通过", "必要时减速或停车让行。", 4, 12)
            )
            : steps(
                step("slow_down", SLOW_DOWN, "提前减速", "进入路口前提前减速。", 1, 10),
                step("observe", OBSERVE_LEFT_RIGHT, "观察左右交通情况", "观察车辆、行人和非机动车。", 2, 10),
                step("lane", KEEP_STRAIGHT, "按导向车道行驶", "按导向车道和标线行驶。", 3, 12),
                step("yield", BRAKE, "按信号灯或让行规则通过", "必要时减速或停车让行。", 4, 12),
                step("signal", id.endsWith("right") ? TURN_SIGNAL_RIGHT : TURN_SIGNAL_LEFT, "转弯开启转向灯", "左转或右转时开启对应转向灯。", 5, 8)
            );
        return project(id, name, description, List.of(intersection),
            intersectionSteps,
            scoreRules(id, rule(id, "left_center", "CONTEXT_TRUE:leftTurnCenterWrong", 10, false, "左转未靠路口中心点左侧转弯，扣 10 分")),
            failRules(id,
                rule(id, "no_slow", "MISSING_STEP:slow_down", 100, true, "不按规定减速，不合格"),
                rule(id, "no_observe", "MISSING_STEP:observe", 100, true, "不观察左右交通情况，不合格"),
                rule(id, "no_yield", "CONTEXT_TRUE:failedToYield", 100, true, "不避让优先通行车辆、行人、非机动车，不合格")
            ),
            tips("路口项目先减速，再观察，再按信号和让行规则通过。")
        );
    }

    private static ExamProject passSlowArea(String id, String name, RoadScene scene) {
        return project(id, name, "通过特殊区域时提前减速观察，必要时停车礼让。", List.of(scene),
            steps(
                step("slow_down", SLOW_DOWN, "提前减速", "进入区域前减速慢行。", 1, 10),
                step("observe", OBSERVE_LEFT_RIGHT, "观察左右交通情况", "观察行人和非机动车。", 2, 10),
                step("yield", STOP, "有行人时停车礼让", "有行人通过时停车礼让。", 3, 12)
            ),
            List.of(),
            failRules(id,
                rule(id, "no_slow", "MISSING_STEP:slow_down", 100, true, "不减速慢行，不合格"),
                rule(id, "no_observe", "MISSING_STEP:observe", 100, true, "不观察左右交通情况，不合格"),
                rule(id, "no_yield", "CONTEXT_TRUE:pedestrianNotYielded", 100, true, "未停车礼让行人，不合格")
            ),
            tips("人行横道、学校、公交站都按减速、观察、礼让处理。")
        );
    }

    private static ExamProject meetingTraffic() {
        String id = "meeting";
        return project(id, "会车", "与对向车辆会车时控制速度和横向安全距离。", List.of(normal_road),
            steps(
                step("observe", OBSERVE_LEFT_RIGHT, "观察对向车辆", "确认会车空间。", 1, 8),
                step("slow_down", SLOW_DOWN, "减速会车", "必要时减速靠右。", 2, 10),
                step("keep_safe", KEEP_STRAIGHT, "保持安全距离", "保持横向安全距离。", 3, 12)
            ),
            scoreRules(id, rule(id, "too_close", "CONTEXT_TRUE:meetingTooClose", 10, false, "会车横向距离不足，扣 10 分")),
            failRules(id, rule(id, "unsafe_meeting", "CONTEXT_TRUE:unsafeMeeting", 100, true, "会车严重影响安全，不合格")),
            tips("会车时要稳、慢、留足横向距离。")
        );
    }

    private static ExamProject overtakingProject() {
        String id = "overtaking";
        return project(id, "超车", "从左侧安全超越并驶回原车道。", List.of(overtaking),
            steps(
                step("distance", OBSERVE_LEFT_RIGHT, "与前车保持安全距离", "确认与前车距离安全。", 1, 8),
                step("left_signal", TURN_SIGNAL_LEFT, "开启左转向灯", "超车前开启左转向灯。", 2, 8),
                step("mirror", CHECK_MIRROR, "观察后视镜", "观察内外后视镜。", 3, 8),
                step("look_back", LOOK_BACK, "回头观察确认安全", "确认左后方安全。", 4, 8),
                step("pass_left", CHANGE_LANE, "从左侧超越", "从左侧完成超越。", 5, 15),
                step("lateral_distance", KEEP_STRAIGHT, "保持横向安全距离", "与被超车辆保持横向安全距离。", 6, 12),
                step("right_signal", TURN_SIGNAL_RIGHT, "开启右转向灯", "准备驶回原车道。", 7, 8),
                step("right_observe", LOOK_BACK, "观察右后方确认安全", "驶回前确认右后方安全。", 8, 8),
                step("return_lane", CHANGE_LANE, "驶回原车道", "平稳驶回原车道。", 9, 12),
                step("signal_off", TURN_SIGNAL_OFF, "关闭转向灯", "超车完成后关闭转向灯。", 10, 8)
            ),
            List.of(),
            failRules(id,
                rule(id, "no_observe", "MISSING_STEP:mirror", 100, true, "超车前未观察，不合格"),
                rule(id, "bad_timing", "CONTEXT_TRUE:overtakeTimingUnsafe", 100, true, "超车时机不合理，不合格"),
                rule(id, "right_overtake", "CONTEXT_TRUE:overtakeFromRight", 100, true, "从右侧超车，不合格"),
                rule(id, "return_no_observe", "MISSING_STEP:right_observe", 100, true, "超车后驶回原车道前未观察，不合格")
            ),
            tips("超车要完整做左灯、观察、回头、左侧超越、右灯、观察、回正。")
        );
    }

    private static ExamProject uTurnProject() {
        String id = "u_turn";
        return project(id, "掉头", "选择允许地点，观察后安全掉头。", List.of(u_turn),
            steps(
                step("observe", OBSERVE_LEFT_RIGHT, "观察交通情况", "观察前后左右交通情况。", 1, 8),
                step("location", CHECK_MIRROR, "选择允许掉头地点", "确认标志标线允许掉头。", 2, 10),
                step("left_signal", TURN_SIGNAL_LEFT, "开启左转向灯", "掉头前开启左转向灯。", 3, 8),
                step("slow_or_stop", SLOW_DOWN, "减速或停车", "按交通情况减速或停车。", 4, 10),
                step("u_turn", U_TURN, "安全掉头", "确认安全后完成掉头。", 5, 15)
            ),
            scoreRules(id, rule(id, "minor_obstruction", "CONTEXT_TRUE:minorObstruction", 10, false, "轻微妨碍其他车辆和行人通行，扣 10 分")),
            failRules(id,
                rule(id, "bad_location", "CONTEXT_TRUE:uTurnLocationWrong", 100, true, "掉头地点选择不当，不合格"),
                rule(id, "no_signal", "MISSING_STEP:left_signal", 100, true, "掉头前未开启左转向灯，不合格"),
                rule(id, "serious_obstruction", "CONTEXT_TRUE:seriousObstruction", 100, true, "严重妨碍其他车辆和行人通行，不合格")
            ),
            tips("看清允许掉头标志标线，先灯后看再动作。")
        );
    }

    private static ExamProject nightLightProject() {
        String id = "night_light";
        return project(id, "模拟夜间灯光使用", "根据语音指令选择正确灯光。", List.of(night_light),
            steps(
                step("low_beam", LOW_BEAM, "近光灯", "夜间会车或照明良好道路使用近光灯。", 1, 8),
                step("high_beam", HIGH_BEAM, "远光灯", "照明不良且无会车时使用远光灯。", 2, 8),
                step("alternate", ALTERNATE_HIGH_LOW_BEAM, "远近光交替", "通过急弯、坡路、拱桥、人行横道、无信号灯路口时交替使用。", 3, 8),
                step("hazard", HAZARD_LIGHT, "危险报警闪光灯", "临时停车或故障场景使用。", 4, 8),
                step("fog", FOG_LIGHT, "雾灯", "雾天或低能见度场景使用。", 5, 8)
            ),
            List.of(),
            failRules(id,
                rule(id, "wrong_light", "CONTEXT_TRUE:wrongLight", 100, true, "不能正确开启灯光，不合格"),
                rule(id, "meeting_not_low", "CONTEXT_TRUE:meetingWithoutLowBeam", 100, true, "会车时不使用近光灯，不合格"),
                rule(id, "high_on_good_light", "CONTEXT_TRUE:highBeamOnGoodLighting", 100, true, "照明良好道路使用远光灯，不合格"),
                rule(id, "no_alternate", "CONTEXT_TRUE:shouldAlternateButNot", 100, true, "特殊路段未交替使用远近光灯，不合格")
            ),
            tips("灯光题跟语音指令走，关键词对应近光、远光、交替、双闪、雾灯。")
        );
    }

    private static ExamProject project(
        String id,
        String name,
        String description,
        List<RoadScene> scenes,
        List<ExamStep> steps,
        List<ScoreRule> scoreRules,
        List<ScoreRule> failRules,
        List<String> tips
    ) {
        return new ExamProject(id, cn(name), cn(description), "default", ALL_VEHICLES, scenes, steps, scoreRules, failRules, tips);
    }

    private static List<ExamStep> steps(ExamStep... steps) {
        return List.of(steps);
    }

    private static ExamStep step(String id, ActionType actionType, String title, String description, int orderIndex, int timeWindowSeconds) {
        return step(id, actionType, title, description, orderIndex, timeWindowSeconds, ALL_VEHICLES, List.of());
    }

    private static ExamStep step(
        String id,
        ActionType actionType,
        String title,
        String description,
        int orderIndex,
        int timeWindowSeconds,
        List<VehicleType> vehicleTypes,
        List<RoadScene> scenes
    ) {
        return new ExamStep(
            id,
            actionType,
            cn(title),
            cn(description),
            true,
            orderIndex,
            timeWindowSeconds,
            vehicleTypes,
            scenes,
            cn("操作正确：" + title),
            cn("当前步骤需要：" + title)
        );
    }

    private static List<ScoreRule> scoreRules(String projectId, ScoreRule... rules) {
        return List.of(rules);
    }

    private static List<ScoreRule> failRules(String projectId, ScoreRule... rules) {
        return List.of(rules);
    }

    private static ScoreRule rule(String projectId, String id, String condition, int deduction, boolean isFail, String message) {
        return new ScoreRule(id, projectId, condition, deduction, isFail, cn(message));
    }

    private static List<String> tips(String... tips) {
        return List.of(tips).stream().map(Subject3ProjectConfig::cn).toList();
    }

    private static String cn(String value) {
        if (value == null || !looksMojibake(value)) {
            return value;
        }
        return new String(value.getBytes(Charset.forName("GBK")), StandardCharsets.UTF_8);
    }

    private static boolean looksMojibake(String value) {
        return value.contains("鐩") || value.contains("璺") || value.contains("鎵") || value.contains("涓")
            || value.contains("杞") || value.contains("绯") || value.contains("閫") || value.contains("鍏");
    }
}
