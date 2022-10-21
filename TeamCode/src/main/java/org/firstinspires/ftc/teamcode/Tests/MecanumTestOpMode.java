package org.firstinspires.ftc.teamcode.Tests;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

@TeleOp
@Config
public class MecanumTestOpMode extends LinearOpMode
{
    public double leftStickY;
    public double leftStickX;
    public double rightStickX;
    public double FL_power;
    public double FR_power;
    public double RL_power;
    public double RR_power;

    public static double P = 0.04;
    public static double I = 0;
    public static double D = 0;

    private double integral, previous_error = 0;

    public double denominator;

    private BNO055IMU imu;
    private DcMotor fr;
    private DcMotor rr;
    private DcMotor fl;
    private DcMotor rl;

    private double desiredAngle = 0;
    private String turnState = "auto";

    private enum DriveMode {
        DRIVER_CONTROLLED,
        AUTO_CONTROL
    }

    private DriveMode driveState = DriveMode.AUTO_CONTROL;

    private final ElapsedTime eTime = new ElapsedTime();

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "AdafruitIMUCalibration.json"; // see the calibration sample op mode
        parameters.mode = BNO055IMU.SensorMode.IMU;
        // parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        fr = hardwareMap.get(DcMotor.class, "frMotor");
        rr = hardwareMap.get(DcMotor.class, "rrMotor");
        fl = hardwareMap.get(DcMotor.class, "flMotor");
        rl = hardwareMap.get(DcMotor.class, "rlMotor");

        fl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        fr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        fl.setDirection(DcMotor.Direction.REVERSE);
        rl.setDirection(DcMotor.Direction.REVERSE);

        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        imu.startAccelerationIntegration(null, null, 1000);

        telemetry.addData("Mode", "waiting for start");
        telemetry.update();

        //Wait for the start button to be pressed.
        waitForStart();

        telemetry.addData("Mode", "running");
        telemetry.update();

        eTime.reset();

        while (opModeIsActive()) {
            controls();
        }
    }

    public void controls() {
        holonomicFormula();
        telemetry.update();

        eTime.reset();
    }

    public void holonomicFormula() {
        double time = eTime.time();
        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);

        leftStickX *= 1.1;

        double gyro_radians = angles.firstAngle * Math.PI/180;
        double newForward = leftStickY * Math.cos(gyro_radians) + leftStickX * Math.sin(gyro_radians);
        double newStrafe = -leftStickY * Math.sin(gyro_radians) + leftStickX * Math.cos(gyro_radians);

        double x1 = desiredAngle - angles.firstAngle;
        double x2;
        if (x1 < 0) {
            x2 = x1 + 360;
        } else {
            x2 = x1 - 360;
        }
        double error = Math.abs(x1) < Math.abs(x2) ? x1 : x2;

        telemetry.addData("Turn Error", error);

        integral += (error * time);
        eTime.reset();

        double derivative = (error - previous_error) / time;
        double rcw = P * -error + I * integral + D * derivative;

        previous_error = error;

        telemetry.addData("Read angle", angles.firstAngle);
        telemetry.addData("RCW", rcw);
        telemetry.addData("Desired Angle", desiredAngle);
        telemetry.addData("rightStickX", rightStickX);

        switch (driveState) {
            case AUTO_CONTROL:
                if (!gamepad1.right_bumper) {
                    if (gamepad1.dpad_up) {
                        desiredAngle = 0;
                    }
                    if (gamepad1.dpad_right) {
                        desiredAngle = 270;
                    }
                    if (gamepad1.dpad_down) {
                        desiredAngle = 180;
                    }
                    if (gamepad1.dpad_left) {
                        desiredAngle = 90;
                    }
                }
                if (rightStickX != 0) {
                    driveState = DriveMode.DRIVER_CONTROLLED;
                }
                turnState = "auto";
                //denominator = Math.max(Math.abs(newForward) + Math.abs(newStrafe) + Math.abs(rcw), 1);
                FL_power = (-newForward + newStrafe + rcw);// / denominator;
                RL_power = (-newForward - newStrafe + rcw);// / denominator;
                FR_power = (-newForward - newStrafe - rcw);// / denominator;
                RR_power = (-newForward + newStrafe - rcw);// / denominator;
                break;
            case DRIVER_CONTROLLED:
                turnState = "driver";
                denominator = Math.max(Math.abs(newForward) + Math.abs(newStrafe) + Math.abs(rightStickX), 1);
                FL_power = (-newForward + newStrafe + rightStickX) / denominator;
                RL_power = (-newForward - newStrafe + rightStickX) / denominator;
                FR_power = (-newForward - newStrafe - rightStickX) / denominator;
                RR_power = (-newForward + newStrafe - rightStickX) / denominator;
                if (!gamepad1.right_bumper) {
                    if (gamepad1.dpad_up) {
                        desiredAngle = 0;
                        driveState = DriveMode.AUTO_CONTROL;
                    }
                    if (gamepad1.dpad_right) {
                        desiredAngle = 270;
                        driveState = DriveMode.AUTO_CONTROL;
                    }
                    if (gamepad1.dpad_down) {
                        desiredAngle = 180;
                        driveState = DriveMode.AUTO_CONTROL;
                    }
                    if (gamepad1.dpad_left) {
                        desiredAngle = 90;
                        driveState = DriveMode.AUTO_CONTROL;
                    }
                }
                break;
        }

        telemetry.addData("turnState", turnState);

        if (gamepad1.left_bumper) {
            FL_power /= 4;
            FR_power /= 4;
            RL_power /= 4;
            RR_power /= 4;
        }

        telemetry.addData("FLpower", FL_power);
        telemetry.addData("FRpower", FR_power);
        telemetry.addData("RLpower", RL_power);
        telemetry.addData("RRpower", RR_power);

        fl.setPower(FL_power);
        fr.setPower(FR_power);
        rl.setPower(RL_power);
        rr.setPower(RR_power);
    }
}