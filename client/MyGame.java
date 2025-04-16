package client;

import tage.*;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.shapes.*;
import tage.nodeControllers.*;
import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.joml.*;
import net.java.games.input.Event;

/**
 *  This the main class that initializes the game and houses all necessary variables
 */
public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	
	private InputManager im;

	private double currTime, prevTime, elapsedTime;
	private HUDCooldown hudCD;
	private boolean paused=false;

	private GameObject gBall, course1, camOb, terr;
	private ObjShape gBallS, cubeS, course1S, terrS;
	private TextureImage gBallT, gBallT2, course1tex, cleartx, grass, hills;
	private Light light1;
	String hudStatus;
	int score;
	private CameraOrbit3D co3d;
	private int skybox;
	private boolean debugmode = false;
	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol = ProtocolType.UDP;
	private ClientProtocol protClient;
	private boolean isClientConnected = false;
	

	public MyGame(String serverAddress, int serverPort) { 
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]));
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	gBallS = new ImportedModel("golfball.obj");
		course1S = new ImportedModel("course1.obj");
		cubeS = new Cube();
		terrS = new TerrainPlane(1000);
	}

	@Override
	public void loadTextures()
	{	gBallT = new TextureImage("golfball.png");
		gBallT2 = new TextureImage("golfball2.png");
		course1tex = new TextureImage("course1tex.png");
		cleartx = new TextureImage("blank.jpg");	// for placeholders
		grass = new TextureImage("grass.png");
		hills = new TextureImage("hills.jpg");
	}

	@Override
	public void loadSkyBoxes() {
		skybox = (engine.getSceneGraph()).loadCubeMap("skybox");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(skybox);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}
	
	@Override
	public void buildObjects()	// V = y, U = x, N = -z
	{	Matrix4f initialTranslation, initialScale;
	
		// player golf ball
		gBall = new GameObject(GameObject.root(), gBallS, gBallT);
		initialTranslation = (new Matrix4f()).translation(0,1,0);
		initialScale = (new Matrix4f()).scaling(1.0f);
		gBall.setLocalTranslation(initialTranslation);
		gBall.setLocalScale(initialScale);
		
		// camera object
		camOb = new GameObject(GameObject.root(), cubeS, cleartx);
		initialTranslation = (new Matrix4f().translation(0, 1, 5));
		initialScale = (new Matrix4f()).scaling(0.1f);
		camOb.setLocalTranslation(initialTranslation);
		camOb.setLocalScale(initialScale);
		camOb.getRenderStates().disableRendering();
		camOb.lookAt(gBall);
		
		// terrain
		terr = new GameObject(GameObject.root(), terrS, grass);
		initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		terr.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(200.0f, 10.0f, 200.0f);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(hills);
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(32);
// DEBUG	terr.getRenderStates().disableRendering();
		
		// Course 1
		course1 = new GameObject(GameObject.root(), course1S, course1tex);initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		initialTranslation = (new Matrix4f()).translation(0.0f, 2.0f, 0.0f);
		course1.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(1.0f);
		course1.setLocalScale(initialScale);
		course1.getRenderStates().setRenderHiddenFaces(true);
	}

	@Override
	public void initializeLights()
	{	
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	// set initial values
		prevTime = System.currentTimeMillis();
		currTime = System.currentTimeMillis();
		elapsedTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);
		score = 0;
		hudStatus = "Ride your faithful dolphin and disarm the satellites!";
		hudCD = new HUDCooldown();
		hudCD.setCooldown(5);
		
		// input manager and functions
		im = engine.getInputManager();	
		
		Key_Move_Fwd kmf = new Key_Move_Fwd();
		Key_Move_Bwd kmb = new Key_Move_Bwd();
		Key_TurnL ktl = new Key_TurnL();
		Key_TurnR ktr = new Key_TurnR();
		ZoomIn zi = new ZoomIn();
		ZoomOut zo = new ZoomOut();
		KeyCamLeft kcl = new KeyCamLeft();
		KeyCamRight kcr = new KeyCamRight();
		Key_RotUp kru = new Key_RotUp();
		Key_RotDown krd = new Key_RotDown();
		FlyUp fu = new FlyUp();
		FlyDown fd = new FlyDown();
		ToggleDebug td = new ToggleDebug();
		
		// keyboard controls
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, kmf, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, kmb, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, ktl, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, ktr, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.Q, zi, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.E, zo, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.LEFT, kcl, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.RIGHT, kcr, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.UP, kru, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.DOWN, krd, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.R, fu, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.F, fd, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.SPACE, td, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		
		// gamepad controls (only loads if a gamepad is present)
		String gp = im.getFirstGamepadName();
		if (!gp.equalsIgnoreCase("")) {
			LStickX lStickX = new LStickX();
			LStickY lStickY = new LStickY();
			RStickX rStickX = new RStickX();
			RStickY rStickY = new RStickY();
			
			im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.X, lStickX, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.Y, lStickY, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.Z, rStickX, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.RZ, rStickY, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._5, zi, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateAction(gp, net.java.games.input.Component.Identifier.Button._7, zo, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		}


		// ------------- positioning the camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		
		
		// node controllers
		
		
		// orbiting camera handler
		co3d = new CameraOrbit3D(engine, camOb, gBall);
		setupNetworking();
	}
	
	private void setupNetworking() {
		isClientConnected = false;
		try { 	protClient = new ClientProtocol(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this); } 
		catch (UnknownHostException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		if (protClient == null) { System.out.println("missing protocol host"); }
		else{ 
			// ask client protocol to send initial join message
			// to server, with a unique identifier for this client
			protClient.sendJoinMessage();
		}
	}

	@Override
	public void update() {
		prevTime = currTime;
		currTime = System.currentTimeMillis();
		if (!paused) elapsedTime += (currTime - prevTime) / 1000;
		
		im.update((float)elapsedTime);
			
		// build and set HUD
		int h = (int) (engine.getRenderSystem().getViewport("MAIN").getActualHeight());
		int w = (int) (engine.getRenderSystem().getViewport("MAIN").getActualWidth());
		String currScore = Integer.toString(score);
		String scoreS = "Score = " + currScore;
		
		if (debugmode) hudStatus = "Debug Mode";
		else hudStatus = "";
		Vector3f hud1Color = new Vector3f(1, .5f, 0);
		Vector3f hud2Color = new Vector3f(.3f,.3f, 1);
		
		
		(engine.getHUDmanager()).setHUD1(scoreS, hud1Color, w/32, h/32);
		(engine.getHUDmanager()).setHUD2(hudStatus, hud2Color, (int) (w*.6), h/32);
		
		if (!debugmode) {
			Vector3f loc = gBall.getWorldLocation();
			float height = terr.getHeight(loc.x(), loc.z()) + 3.2f;
			gBall.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));
		}
		
		co3d.updateCamPosition();
		updateCam();
		processNetwork((float) elapsedTime);
	}
	
	protected void processNetwork(float elapsedTime) {
		if (protClient != null) protClient.processPackets();
	}

	@Override
	public void keyPressed(KeyEvent e)	// V = y, U = x, N = -z
	{	
		super.keyPressed(e);
	}

	/**
	 * 	An input action class for when the left control stick is moved left or right. Now updated to use global yaw.
	 * 	In this case, doing so rotates the view left or right.
	 */
	public class LStickX extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				gBall.globalYaw(e.getValue()/-30.0f);
				if (isClientConnected) protClient.sendMoveMessage(gBall.getWorldLocation());
			}
		}
	}
	/**
	 * 	An input action class for when the left control stick is moved up or down.
	 * 	In this case, it moves the camera or dolphin forward or backward
	 */
	public class LStickY extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				gBall.moveZ(e.getValue()/-10.0f);
				if (isClientConnected) protClient.sendMoveMessage(gBall.getWorldLocation());
			}
		}
	}
	
	/**
	 * 	An input action class for when the left control stick is moved left or right. Now updated to use global yaw.
	 * 	In this case, it rotates the view left or right.
	 */
	public class RStickX extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				co3d.xzRot(-1.0f*e.getValue());
			}
		}
	}
	/**
	 * 	An input action class for when the right control stick is moved up or down.
	 * 	In this case, it rotates the view up or down.
	 */
	public class RStickY extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			if (Math.abs((double)e.getValue()) >= 0.2) {
				co3d.yRot(e.getValue());
			}
		}
	}
	/**
	 * An input action class for moving forward with the keyboard.
	 */
	public class Key_Move_Fwd extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			gBall.moveZ(1/10f);
			if (isClientConnected) protClient.sendMoveMessage(gBall.getWorldLocation());
		}
	}
	/**
	 *  An input action class for moving backwards with the keyboard.
	 */
	public class Key_Move_Bwd extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			gBall.moveZ(-1/10f);
			if (isClientConnected) protClient.sendMoveMessage(gBall.getWorldLocation());
		}
	}
	/**
	 *  An input action class for rotating the view to the left with the keyboard. Now updated to use global yaw.
	 */
	public class Key_TurnL extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			gBall.globalYaw(1/60f);
		}
	}
	/**
	 * 	An input action class for rotating the view to the right with the keyboard. Now updated to use global yaw.
	 */
	public class Key_TurnR extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			gBall.globalYaw(-1/60f);
		}
	}
	/**
	 * 	An input action class for rotating the view upward using the keyboard.
	 */
	public class Key_RotUp extends AbstractInputAction{
		@Override
		public void performAction(float time, Event e) {
			co3d.yRot(-1.0f);
		}
	}
	/**
	 * 	An input action class for rotating the view downward using the keyboard.
	 */
	public class Key_RotDown extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.yRot(1.0f);
		}
	}
	/**
	 * 	An action class for orbiting the camera left with the keyboard/
	 */
	public class KeyCamLeft extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.xzRot(1.0f);
		}
	}
	/**
	 * 	An action class for orbiting the camera right with the keyboard/
	 */
	public class KeyCamRight extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.xzRot(-1.0f);
		}
	}
	/**
	 * 	An action class for zooming the camera in closer
	 */
	public class ZoomIn extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.zoom(-0.1f);
		}
	}
	/**
	 * 	An action class for zooming the camera farther away
	 */
	public class ZoomOut extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			co3d.zoom(0.1f);
		}
	}
	/**
	 * An action class for debug mode to move the avatar up
	 */
	public class FlyUp extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			if (debugmode) {
				gBall.moveY(1/10.0f);
				if (isClientConnected) protClient.sendMoveMessage(gBall.getWorldLocation());
			}
		}
	}
	/**
	 * An action class for debug mode to move the avatar down
	 */
	public class FlyDown extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			if (debugmode) {
				gBall.moveY(-1/10.0f);
				if (isClientConnected) protClient.sendMoveMessage(gBall.getWorldLocation());
			}
		}
	}
	/**
	 * An action class for turning debug mode on/off
	 */
	public class ToggleDebug extends AbstractInputAction{
		@Override
		public void performAction(float time, Event evt) {
			debugmode = !debugmode;
		}
	}
	

		/* 	--D-pad values--
		 * 	Up/Left: 	0.125
		 * 	Up: 		0.25
		 * 	Up/Right:	0.375
		 * 	Right: 		0.5
		 * 	Down/Right:	0.625
		 * 	Down:		0.75
		 * 	Down/Left:	0.875
		 * 	Left: 		1.0		*/

	private class Disconnect extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt) { 
			if(protClient != null && isClientConnected == true) { protClient.sendByeMessage(); }
		}
	}

	/**
	 * 	A void function called by the update() function every time.
	 * 	It moves the view camera into the same position and rotation as the "CamOb" object, which is not rendered.
	 * 	This is done to simplify camera movement functions.
	 */
	public void updateCam() {
		Vector3f loc, fwd, up, right;
		Camera cam;
		cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		loc = camOb.getWorldLocation();
		fwd = camOb.getWorldForwardVector();
		up = camOb.getWorldUpVector();
		right = camOb.getWorldRightVector();
		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		cam.setLocation(loc);
	}
	
	/**
	 * 	A custom object used to allow the game HUD to not update until a specified time elapses.
	 */
	public class HUDCooldown{
		double cooldownTime;
		
		public HUDCooldown() {
			this.cooldownTime = 0;
		}
		/**
		 * sets the timer during which the HUD won't update.
		 * @param time The time in seconds before the HUD can update again.
		 */
		public void setCooldown(int time) {
		cooldownTime = elapsedTime + time;
		}
		/**
		 * Returns a boolean informing other functions whether the HUD may be updated.
		 * @return True of enough time has passed, false if not
		 */
		public boolean checkCooldown() {
			if (elapsedTime >= cooldownTime) return true;
			else return false;
		}
	}
	/**
	 * Tells other functions whether two GameObjects are a specified distance from each other.
	 * Makes use of the getDist function.
	 * @param o1 The first GameObject to compare.
	 * @param o2 The second GameObject to compare.
	 * @param dist The distance you'd like to check whether the two GameObjects are within.
	 * @return True if the two objects are closer then the specified distance, or false if they are not.
	 */
	public boolean checkDist(GameObject o1, GameObject o2, float dist) {
		float temp = getDist(o1, o2);
		if (temp <= dist) return true;
		else return false;
	}
	/**
	 * Returns the distance between two GameObjects.
	 * @param o1 The first GameObject to compare.
	 * @param o2 The second GameObject to compare.
	 * @return the distance between the two objects.
	 */
	public float getDist(GameObject o1, GameObject o2) {
		Vector3f loc1, loc2;
		float x1, y1, z1, x2, y2, z2;
		loc1 = o1.getWorldLocation();
		x1 = loc1.x();
		y1 = loc1.y();
		z1 = loc1.z();
		loc2 = o2.getWorldLocation();
		x2 = loc2.x();
		y2 = loc2.y();
		z2 = loc2.z();
		
		return (float) Math.sqrt(((x2-x1)*(x2-x1))+((y2-y1)*(y2-y1))+((z2-z1)*(z2-z1)));
	}
	
	public void setConnected(boolean b) {
		isClientConnected = b;
	}
	
	public GameObject getAvatar() { return gBall; }
	public Vector3f getAvatarPosition() { return gBall.getWorldLocation(); }
	public ObjShape getGhostShape() { return gBallS; }
	public TextureImage getGhostTexture() { return gBallT2; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
}