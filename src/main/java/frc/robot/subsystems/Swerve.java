package frc.robot.subsystems;

import frc.robot.SwerveModule;
import frc.robot.constants;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;

import com.ctre.phoenix6.configs.Pigeon2Configuration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.pathplanner.lib.auto.AutoBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;


public class Swerve extends SubsystemBase {
    public SwerveDriveOdometry swerveOdometry;

    public SwerveModule[] mSwerveMods;
    public Pigeon2 gyro;

    //private Field2d field = new Field2d();

    public Swerve() {
        gyro = new Pigeon2(constants.Swerve.pigeonID);
        gyro.getConfigurator().apply(new Pigeon2Configuration());
        //gyro.setYaw(0);

        


        mSwerveMods = new SwerveModule[] {
            new SwerveModule(0, constants.Swerve.Mod0.constants),
            new SwerveModule(1, constants.Swerve.Mod1.constants),
            new SwerveModule(2, constants.Swerve.Mod2.constants),
            new SwerveModule(3, constants.Swerve.Mod3.constants)
        };
        swerveOdometry = new SwerveDriveOdometry(constants.Swerve.swerveKinematics, getGyroYaw(), getModulePositions());


         AutoBuilder.configureHolonomic(
            this::getPoseAutonomous,
            this::resetPose,
            this::getSpeeds,
            this::driveRobotRelative,
            constants.Swerve.pathFollowerConfig,
             // Adjust config values as needed
            () -> { // Boolean supplier that controls when the path will be mirrored for the red alliance
              // This will flip the path being followed to the red side of the field.
              // THE ORIGIN WILL REMAIN ON THE BLUE SIDE
              return false;
             }, // Mirroring logic for red alliance
            this
            
        );

        // Set up custom logging to add the current path to a field 2d widget
       // PathPlannerLogging.setLogActivePathCallback((poses) -> field.getObject("path").setPoses(poses));

        //SmartDashboard.putData("Field", field);
    }
     @Override
    public void periodic(){
        swerveOdometry.update(getGyroYaw(), getModulePositions());  

        for(SwerveModule mod : mSwerveMods){
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Cancoder", mod.getCANcoder().getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Integrated", mod.getPosition().angle.getDegrees());
            SmartDashboard.putNumber("Mod " + mod.moduleNumber + " Velocity", mod.getState().speedMetersPerSecond);    
        }
        //field.setRobotPose(getPose());
        //System.out.println(gyro.getAccelerationX());
        //System.out.println(gyro.getAccelerationY());
    }

   public void drive(Translation2d translation, double rotation, boolean fieldRelative, boolean isOpenLoop) {
    rotation = -rotation;
    SwerveModuleState[] swerveModuleStates = 
        constants.Swerve.swerveKinematics.toSwerveModuleStates(
            fieldRelative ? ChassisSpeeds.fromFieldRelativeSpeeds(
                    translation.getX(),
                    translation.getY(),
                    rotation,
                    getHeading()
                )
                : new ChassisSpeeds(
                    translation.getX(),
                    translation.getY(),
                        rotation)
                );
    SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, constants.Swerve.maxSpeed);

    for (SwerveModule mod : mSwerveMods) {
        mod.setDesiredState(swerveModuleStates[mod.moduleNumber], isOpenLoop);
    }
} 

 /* Used by SwerveControllerCommand in Auto */
 public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, constants.Swerve.maxSpeed);
    
    for(SwerveModule mod : mSwerveMods){
        mod.setDesiredState(desiredStates[mod.moduleNumber], false);
    }
}

public SwerveModuleState[] getModuleStates(){
    SwerveModuleState[] states = new SwerveModuleState[4];
    for(SwerveModule mod : mSwerveMods){
        states[mod.moduleNumber] = mod.getState();
    }
    return states;
}

public SwerveModulePosition[] getModulePositions(){
    SwerveModulePosition[] positions = new SwerveModulePosition[4];
    for(SwerveModule mod : mSwerveMods){
        positions[mod.moduleNumber] = mod.getPosition();
    }
    return positions;
}

public Pose2d getPose() {
    return swerveOdometry.getPoseMeters();
}

public Pose2d getPoseAutonomous() {
    Pose2d temp = swerveOdometry.getPoseMeters();
    return new Pose2d(swerveOdometry.getPoseMeters().getTranslation(), new Rotation2d(-temp.getRotation().getRadians()));
}
//there is a very low chance anybody else will see these comments
public void setPose(Pose2d pose) {
    swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), pose);
}

public Rotation2d getHeading(){
    return getPose().getRotation();
}

public void setHeading(Rotation2d heading){
    swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), heading));
}

public void zeroHeading(){
    swerveOdometry.resetPosition(getGyroYaw(), getModulePositions(), new Pose2d(getPose().getTranslation(), new Rotation2d()));
}

public Rotation2d   getGyroYaw() {
    return Rotation2d.fromDegrees((gyro.getYaw().getValue()) );
}

public void resetModulesToAbsolute(){
    for(SwerveModule mod : mSwerveMods){
        mod.resetToAbsolute();
    }
}

     public ChassisSpeeds getButton(Translation2d translation, double rotation, boolean fieldRelative,boolean halfSpeed){
        if(fieldRelative && halfSpeed){
           return ChassisSpeeds.fromFieldRelativeSpeeds(
                (translation.getX()/3), 
                (translation.getY()/3),
                (rotation/3), 
                getGyroYaw()
                );
        }
    else if(!fieldRelative && halfSpeed){
            return new ChassisSpeeds(
                (translation.getX()/3), 
                (translation.getY()/3),
                (rotation/3)
            );
        }
    else if(fieldRelative && !halfSpeed){
        return ChassisSpeeds.fromFieldRelativeSpeeds(
                        translation.getX(), 
                        translation.getY(), 
                        rotation, 
                        getGyroYaw()
                    );
        }
    else{
        return new ChassisSpeeds(
                        translation.getX(), 
                        translation.getY(), 
                        rotation);
        }
    }
    public void resetPose(Pose2d pose) {
        swerveOdometry.resetPosition((getGyroYaw()), getModulePositions(), pose);
    }

    public ChassisSpeeds getRobotRelativeSpeeds(ChassisSpeeds fieldRelativeSpeeds) {
        return ChassisSpeeds.fromFieldRelativeSpeeds(
            fieldRelativeSpeeds.vxMetersPerSecond ,
            fieldRelativeSpeeds.vyMetersPerSecond ,
            fieldRelativeSpeeds.omegaRadiansPerSecond,
            getHeading()
        );
    }
    public ChassisSpeeds getSpeeds() {
        return (constants.Swerve.swerveKinematics.toChassisSpeeds(getModuleStates()));
      }
    

    public void driveRobotRelative(ChassisSpeeds robotRelativeSpeeds) {
        SwerveModuleState[] targetStates = constants.Swerve.swerveKinematics.toSwerveModuleStates(robotRelativeSpeeds);
        setStates(targetStates);
    }

    public void setStates(SwerveModuleState[] targetStates) {
        // Increase the value as needed (e.g., 1.0 for 1 m/s)
        SwerveDriveKinematics.desaturateWheelSpeeds(targetStates, 4.5); 
        
        for (int i = 0; i < mSwerveMods.length; i++) {
          mSwerveMods[i].setDesiredState(targetStates[i], false);
        }
      }

     
    
      public SwerveModulePosition[] getPositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[mSwerveMods.length];
        for (int i = 0; i < mSwerveMods.length; i++) {
          positions[i] = mSwerveMods[i].getPosition();
        }
        return positions;
      }
    
    
      


      
    

    

    

   

    

    
}