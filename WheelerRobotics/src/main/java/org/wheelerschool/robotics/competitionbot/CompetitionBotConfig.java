package org.wheelerschool.robotics.competitionbot;

import com.qualcomm.hardware.adafruit.BNO055IMU;
import com.qualcomm.hardware.adafruit.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;

import org.wheelerschool.robotics.library.util.DcMotorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration values for the competition bot.
 *
 * @author luciengaitskell
 * @since 161216
 */

public class CompetitionBotConfig {
    private HardwareMap hardwareMap;

    public class AdvancedServo {
        public Servo servo;
        private double retractedPos;
        private double extendedPos;

        public AdvancedServo(Servo servo) {
            this.servo = servo;
        }

        public void activatePusher(boolean extend) {
            if (extend) {
                servo.setPosition(extendedPos);
            } else {
                servo.setPosition(retractedPos);
            }
        }
    }

    // IMU:
    public BNO055IMU imu;

    // Autonomous Left/Right:
    public AdvancedServo pusherLeft;
    public AdvancedServo pusherRight;

    public static boolean defaultRobotForwards = true;
    public boolean robotForwards = defaultRobotForwards;
    public List<DcMotor> leftMotors = new ArrayList<>();
    public List<DcMotor> rightMotors = new ArrayList<>();

    public List<DcMotor> launcherMotors = new ArrayList<>();
    public enum LauncherMotorsState {
        DISABLE, IDLE, LAUNCH
    }

    public CRServo feederServo;

    public OpticalDistanceSensor feedDetector;

    public ColorSensor colorRight;
    public ColorSensor colorLeft;

    public OpticalDistanceSensor groundReflectSensor;

    public CompetitionBotConfig(HardwareMap hardwareMap) {
        this(hardwareMap, defaultRobotForwards);
    }

    public CompetitionBotConfig(HardwareMap hardwareMap, boolean robotForwards) {
        this.hardwareMap = hardwareMap;

        //  Sensors:
        //      IMU:
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "AdafruitIMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        //          Retrieve and initialize the IMU:
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);


        // Pusher Servos:
        this.pusherLeft = new AdvancedServo(hardwareMap.servo.get("pusherLeft"));
        this.pusherLeft.retractedPos = 0;
        this.pusherLeft.extendedPos = 1;
        this.pusherRight = new AdvancedServo(hardwareMap.servo.get("pusherRight"));
        this.pusherRight.servo.setDirection(Servo.Direction.REVERSE);
        this.pusherRight.retractedPos = 0;
        this.pusherRight.extendedPos = 1;

        // Drive Motors:
        this.leftMotors.add(this.hardwareMap.dcMotor.get("backLeft"));
        this.rightMotors.add(this.hardwareMap.dcMotor.get("backRight"));
        // Set robot direction:
        setRobotDirection(robotForwards);

        DcMotorUtil.setMotorsRunMode(this.leftMotors, DcMotor.RunMode.RUN_USING_ENCODER);
        DcMotorUtil.setMotorsRunMode(this.rightMotors, DcMotor.RunMode.RUN_USING_ENCODER);

        // Launcher Motors:
        launcherMotors.add(hardwareMap.dcMotor.get("launcherLeft"));
        launcherMotors.add(hardwareMap.dcMotor.get("launcherRight"));
        DcMotorUtil.setMotorsDirection(launcherMotors, DcMotorSimple.Direction.REVERSE);

        // Launcher Feed Servo:
        feederServo = hardwareMap.crservo.get("feeder");

        feedDetector = hardwareMap.opticalDistanceSensor.get("feedDetector");

        // Beacon Sensors:
        /// NOTE: Color sensors are flipped, because they are on a side referenced to the front
        ///     of the robot:
        colorLeft = hardwareMap.colorSensor.get("colorLeft");
        colorLeft.setI2cAddress(I2cAddr.create7bit(0x1e));
        colorLeft.enableLed(false);
        colorRight = hardwareMap.colorSensor.get("colorRight");
        colorRight.setI2cAddress(I2cAddr.create7bit(0x1f));
        colorRight.enableLed(false);

        // Line Detector:
        groundReflectSensor = hardwareMap.opticalDistanceSensor.get("groundODS");
    }

    public void setRobotDirection(boolean forwards) {
        if (robotForwards != forwards) {
            robotForwards = forwards;
            List<DcMotor> newRightMotors = this.leftMotors;
            List<DcMotor> newLeftMotors = this.rightMotors;
            this.leftMotors = newLeftMotors;
            this.rightMotors = newRightMotors;
        }

        DcMotorUtil.setMotorsDirection(this.leftMotors, DcMotorSimple.Direction.FORWARD);
        DcMotorUtil.setMotorsDirection(this.rightMotors, DcMotorSimple.Direction.REVERSE);
    }

    public double setLauncherState(LauncherMotorsState state) {
        double launcherMotorIdlePower = 0.1;
        double launcherMotorLaunchPower = 0.2;

        // Default launcher speed:
        double launcherPower = 0;
        if (state == LauncherMotorsState.IDLE){
            // Set the launcher power to idle mode:
            launcherPower = launcherMotorIdlePower;
        } else if (state == LauncherMotorsState.LAUNCH) {
            // Set the launcher power to launch mode:
            launcherPower = launcherMotorLaunchPower;
        }

        // Set the launcher Motors:
        DcMotorUtil.setMotorsPower(this.launcherMotors, launcherPower);

        return launcherPower;
    }
}
