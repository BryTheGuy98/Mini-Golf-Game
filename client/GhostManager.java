package client;

import java.io.IOException;
import java.util.*;
import org.joml.*;
import tage.*;

public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvs = new Vector<GhostAvatar>();
	
	public GhostManager(VariableFrameRateGame vfrg) { game = (MyGame)vfrg; }
	
	public void createGhost(UUID id, Vector3f p) throws IOException { 
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, p);
		Matrix4f initialScale = (new Matrix4f()).scaling(0.25f);
		newAvatar.setLocalScale(initialScale);
		ghostAvs.add(newAvatar);
	}
	
	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAv = findAvatar(id);
		if(ghostAv != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghostAv);
			ghostAvs.remove(ghostAv);
		}
		else { System.out.println("unable to find ghost in list"); }
	}
	
	private GhostAvatar findAvatar(UUID id) { 
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvs.iterator();
		while(it.hasNext()) {
			ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0) { 
				return ghostAvatar;
			}
		}
	return null;
	}
	
	public void updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) { ghostAvatar.setPos(position); }
		else { System.out.println("unable to find ghost in list"); }
	}
}