package com.udacity.catpoint.security.service;


import com.udacity.catpoint.security.ImageService.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest
{
    /**
     * Rigorous Test :-)
     */
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    private SecurityService securityService;
    private Sensor sensor;

    @BeforeEach
    void setup(){
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("Test", SensorType.DOOR);
    }

    /**
     * 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @Test
    void AlarmPendingStatus_whenArmedAndSensorActivated(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY, ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm
     * status to alarm.
     */
    @Test
    void AlarmAlarmStatus_whenArmedAndSensorActivatedAndAlreadyPending(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY, ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 3. If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    void NoAlarm_whenPendingAlarmAndInactiveSensors(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 4. If alarm is active, change in sensor state should not affect the alarm state.
     *
     * Arm the system and activate two sensors, the system should go to alarm state.
     * Then deactivate one sensor and the system should not change alarm state
     */
    @Test
    void NoAffectState_whenAlarmActiveChangeSensorState(){
        Sensor sensor1 = new Sensor("test1", SensorType.DOOR);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(sensor);
        sensorSet.add(sensor1);

        for(Sensor s: sensorSet){
            s.setActive(true);
        }
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
    @Test
    void ChangeToAlarm_whenSensorActivatedWhileAlreadyActive(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    void AlarmNoChange_whenSensorDeactivatedWhileAlreadyInactive(){
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * 7. If the image service identifies an image containing a cat while the system is armed-home,
     * put the system into alarm status.
     *
     * Arm the system, scan a picture until it detects a cat, activate a sensor,
     * scan a picture again until there is no cat,
     * the system should still be in alarm state as there is a sensor active
     */
    @Test
    void SystemIntoAlarm_whenCatIdentifiedWhileSystemArmedHome(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void SystemIntoAlarm_whenCatIdentifiedWhileSystemArmedHome2(){
        Sensor sensor1 = new Sensor("test1", SensorType.DOOR);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(sensor);
        sensorSet.add(sensor1);
        for(Sensor s: sensorSet){
            s.setActive(true);
        }
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * 8. If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    void ChangeToNoAlarm_whenCatNotIdentifiedAndSensorsNotActive(){
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0; i < 4; i++){
            sensors.add(new Sensor("test"+i, SensorType.DOOR));
        }
        sensors.forEach(sensor1 -> sensor1.setActive(false));
//        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 9. If the system is disarmed, set the status to no alarm.
     */
    @Test
    void SetStatusNoAlarm_whenSystemDisarmed(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 10. If the system is armed, reset all sensors to inactive.
     *
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void resetSensorsToInactive_whenSystemIsArmed(ArmingStatus status){
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0; i < 4; i++){
            sensors.add(new Sensor("test"+i, SensorType.DOOR));
        }
        sensors.forEach(sensor1 -> sensor1.setActive(true));
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);
        securityService.getSensors().forEach(s -> assertFalse(s.getActive()));
    }

    /**
     * 11. If the system is armed-home while the camera shows a cat,
     * set the alarm status to alarm.
     *
     * Put the system as disarmed, scan a picture until it detects a cat,
     * after that make it armed, it should make system in ALARM state
     */
    @Test
    void SetStatusAlarm_whenArmedHomeWhileShowsCat(){
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void SetStatusAlarm_whenArmedHomeWhileShowsCat2(){
        Set<Sensor> sensors = new HashSet<>();
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Coverage Test1:
     */
    @Test
    void statusListenerTest()
    {
        securityService.addStatusListener(mock(StatusListener.class));
        securityService.removeStatusListener(mock(StatusListener.class));
    }


    /**
     * Coverage Test2:
     */
    @Test
    void sensorTest()
    {
        securityService.addSensor(mock(Sensor.class));
        securityService.removeSensor(mock(Sensor.class));
    }

    /**
     * Coverage Test3:
     */
    @Test
    void NoChange_whenDisarmed()
    {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
}
