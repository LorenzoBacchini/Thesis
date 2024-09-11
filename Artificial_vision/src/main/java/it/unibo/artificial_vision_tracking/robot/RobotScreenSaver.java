package it.unibo.artificial_vision_tracking.robot;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.w3c.dom.DOMException;

import it.unibo.artificial_vision_tracking.aruco_markers.CameraPose;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;

/**
 * class to manage the robot screen saver game.
 */
public class RobotScreenSaver {
    private static final Logger LOGGER = Logger.getLogger(RobotScreenSaver.class.getName());

    //Costants
    private static final int MARKERS_COUNT = 5;
    private static final int TVECS_ROW_FOR_MARKER = 3;
    private static final int OLD_MARKER_POSITION_UPDATE_FREQUENCY = 5;
    private static final int LOW_MOVEMENT_TIME = 1000;
    private static final int HIGH_MOVEMENT_TIME = 2000;
    private static final double BORDER_DELTA = 0.1;

    private ESP32Client client;
    private final Set<Integer> borderCorners;

    /**
     * Constructor of the class.
     * @param uri of the robot
     * @param borderCorners list of the border markers
     */
    public RobotScreenSaver(final String uri, final List<Integer> borderCorners) {
        try {
            final URI serverUri = new URI(uri); // Replace with ESP32 ip address
            client = new ESP32Client(serverUri);
            client.connectBlocking();
        } catch (InterruptedException | URISyntaxException e) {
            LOGGER.warning(e.getMessage());
        }
        this.borderCorners = new HashSet<>(borderCorners);
    }

    /**
     * Screen saver method.
     * This method is used to move the robot based on the markers detected by the camera.
     * The robot will move forward if the zero marker is inside the border markers, 
     * otherwise it will rotate by 90 degrees to the left or the right depending on his direction
     * regard to the border and then continue to move forward.
     * 
     * @param cameraMatrix
     * @param distCoeffs
     * @param markerLength
     * @param dictionaryType
     * @param selectedCamera
     */
    public void screenSaver(final Mat cameraMatrix, final Mat distCoeffs, final float markerLength, 
        final int dictionaryType, final int selectedCamera) {
        double oldZeroMarkerX = 0;
        double oldZeroMarkerY = 0;
        boolean rotationStarted = false;
        long startTimeRotation = 0;
        long endTimeRotation = 0;
        int counter = OLD_MARKER_POSITION_UPDATE_FREQUENCY;
        boolean uTurn = false;

        final CameraPose cp = new CameraPose(cameraMatrix, distCoeffs, markerLength, dictionaryType, selectedCamera);
        final VideoCapture capture = cp.getCamera();
        while (true) {
            final List<Double> x = new ArrayList<>();
            final List<Double> y = new ArrayList<>();
            final Double zeroMarkerX;
            final Double zeroMarkerY;

            final Mat[] status = cp.calcSinglePose(capture);
            final int[] allIdsArray = new int[(int) status[2].total()];
            status[2].get(0, 0, allIdsArray);
            if (!containsAllMarker(allIdsArray)) {
                continue;
            }
            final List<Double> zeroMarkerPosition = extractMapData(x, y, status[0], allIdsArray);
            zeroMarkerX = zeroMarkerPosition.get(0);
            zeroMarkerY = zeroMarkerPosition.get(1);
            //Send the command to the bot
            Collections.sort(x);
            Collections.sort(y);
            double lowestX = x.get(0);
            double highestX = x.get(x.size() - 1);
            double lowestY = y.get(0);
            double highestY = y.get(y.size() - 1);

            lowestX = lowestX + BORDER_DELTA;
            highestX = highestX - BORDER_DELTA;
            lowestY = lowestY + BORDER_DELTA;
            highestY = highestY - BORDER_DELTA;

            if ((zeroMarkerX > lowestX && zeroMarkerX < highestX 
                && zeroMarkerY > lowestY && zeroMarkerY < highestY) 
                || System.currentTimeMillis() - endTimeRotation < 1000) {
                LOGGER.info("Dentro");
                sendMessage(DirectionEnum.MOVE_FORWARD.toString());
            } else if ((zeroMarkerX < lowestX && zeroMarkerY < lowestY) || (zeroMarkerX > highestX && zeroMarkerY < lowestY)
                || (zeroMarkerX < lowestX && zeroMarkerY > highestY) || (zeroMarkerX > highestX && zeroMarkerY > highestY)) {
                uTurn = true;
                LOGGER.info("U-Turn");
                sendMessage(DirectionEnum.TURN_LEFT.toString());
            } else {
                if ((zeroMarkerX > highestX || zeroMarkerX < lowestX) && !rotationStarted) {
                    LOGGER.info("Destra o sinistra");
                    if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY > zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_LEFT.toString());
                    } else if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY < zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_RIGHT.toString());
                    } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY < zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_LEFT.toString());
                    } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY > zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_RIGHT.toString());
                    }
                } else if ((zeroMarkerY > highestY || zeroMarkerY < lowestY) && !rotationStarted) {
                    LOGGER.info("Alto o basso");
                    if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY > zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_RIGHT.toString());
                    } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY > zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_LEFT.toString());
                    } else if (oldZeroMarkerX < zeroMarkerX && oldZeroMarkerY < zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_LEFT.toString());
                    } else if (oldZeroMarkerX > zeroMarkerX && oldZeroMarkerY < zeroMarkerY) {
                        sendMessage(DirectionEnum.TURN_RIGHT.toString());
                    }
                }
                if (!rotationStarted) {
                    LOGGER.info("Rotation started");
                    startTimeRotation = System.currentTimeMillis();
                    rotationStarted = true;
                } else if ((uTurn && System.currentTimeMillis() - startTimeRotation > HIGH_MOVEMENT_TIME)
                    || (!uTurn && System.currentTimeMillis() - startTimeRotation > LOW_MOVEMENT_TIME)) {
                    LOGGER.info("Rotation ended");
                    sendMessage(DirectionEnum.MOVE_FORWARD.toString());
                    rotationStarted = false;
                    uTurn = false;
                    endTimeRotation = System.currentTimeMillis();
                }
            }
            if (counter == OLD_MARKER_POSITION_UPDATE_FREQUENCY) {
                oldZeroMarkerX = zeroMarkerX;
                oldZeroMarkerY = zeroMarkerY;
                counter = 0;
            } else {
                counter++;
            }

            x.clear();
            y.clear();
            status[0].release();
            status[1].release();
            status[2].release();
        }
    }

    private void sendMessage(final String message) {
        try {
            client.send(message);
        } catch (DOMException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    private static boolean containsAllMarker(final int[] allIdsArray) {
        final Set<Integer> ids = new HashSet<>();
        for (final Integer i : allIdsArray) {
            ids.add(i);
        }

        return ids.size() >= MARKERS_COUNT && ids.contains(0);
    }

    private List<Double> extractMapData(final List<Double> x, final List<Double> y, final Mat tvec, final int[] ids) {
        final List<Double> zeroMarkerPosition = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            if (borderCorners.contains(ids[i])) {
                x.add(tvec.get(i * TVECS_ROW_FOR_MARKER, 0)[0]);
                y.add(tvec.get(i * TVECS_ROW_FOR_MARKER + 1, 0)[0]);
            } else if (ids[i] == 0) {
                zeroMarkerPosition.add(tvec.get(i * TVECS_ROW_FOR_MARKER, 0)[0]);
                zeroMarkerPosition.add(tvec.get(i * TVECS_ROW_FOR_MARKER + 1, 0)[0]);
            }
        }
        return zeroMarkerPosition;
    }
}
