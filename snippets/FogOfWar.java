// http://www.jmonkeyengine.com/forum/index.php?topic=4451.15
// http://www.cokeandcode.com/node/1279
package org.newdawn.jme.util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.image.Texture2D;
import com.jme.math.Matrix4f;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.renderer.pass.Pass;
import com.jme.scene.Spatial;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;

/**
 * Fog of war pass.
 * 
 */
public class FogOfWarPass extends Pass {
    private static final long serialVersionUID = 1L;

    private static Matrix4f biasMatrix = new Matrix4f(0.5f, 0.0f, 0.0f, 0.0f,
                                                      0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f,
                                                      1.0f); // bias from [-1, 1] to [0, 1]
    
    /** The size of the fog of war texture */
    private int fowSize = 1024;
    /** The size of each fog of world segment in world coordinates */
    private int scale = 25; 
	
    private Texture2D fowTexture;
    private TextureState fowTextureState;
    private BlendState blendState;
    private LightState noLights;
    private IntBuffer idBuff = BufferUtils.createIntBuffer(16);
    private ByteBuffer imageData;
	
    protected boolean initialised = false;
    private RenderState[] preStates = new RenderState[RenderState.RS_MAX_STATE];

    public FogOfWarPass() {
    }
	
    /**
     * saves any states enforced by the user for replacement at the end of the
     * pass.
     */
    protected void saveEnforcedStates() {
        for (int x = RenderState.RS_MAX_STATE; --x >= 0;) {
            preStates[x] = context.enforcedStateList[x];
        }
    }

    /**
     * replaces any states enforced by the user at the end of the pass.
     */
    protected void replaceEnforcedStates() {
        for (int x = RenderState.RS_MAX_STATE; --x >= 0;) {
            context.enforcedStateList[x] = preStates[x];
        }
    }
	
    public void setVisibility(float xpos, float zpos, float visible) {
        init(DisplaySystem.getDisplaySystem().getRenderer());
		
        xpos /= scale;
        zpos /= scale;
		
        int xp = (fowSize / 2) + (int) (xpos);
        int yp = (fowSize / 2) + (int) (zpos);

        int index = xp+(yp*fowSize);
        imageData.put(index, (byte) (visible*255));
    }
	
    public void updateMap() {
        updateTexture();
    }
	
    public void init(Renderer r) {
        if (initialised) {
            return;
        }

        initialised = true; 
		
        blendState = r.createBlendState();
        blendState.setEnabled(true);
        blendState.setBlendEnabled(true);
        blendState.setSourceFunction(BlendState.SourceFunction.DestinationColor);
        blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		
        fowTexture = new Texture2D();
        fowTexture.setApply(Texture.ApplyMode.Modulate);
        fowTexture.setMinificationFilter(Texture.MinificationFilter.NearestNeighborNoMipMaps); // no
        fowTexture.setWrap(Texture.WrapMode.Clamp); 
        fowTexture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
		
        fowTextureState = r.createTextureState();
        fowTextureState.setTexture(fowTexture);
		
        noLights = r.createLightState();
        noLights.setEnabled(false);
		
        imageData = BufferUtils.createByteBuffer(fowSize*fowSize);
		
        Image image = new Image(Image.Format.Luminance8, fowSize, fowSize, imageData);
        fowTexture.setImage(image);
		
        Matrix4f orthoProjection = new Matrix4f();
        setOrthoMatrix(orthoProjection);
        Matrix4f view = new Matrix4f();
        Quaternion quat = new Quaternion();
        quat.lookAt(new Vector3f(0,-1,0), new Vector3f(0,0,-1));
        quat.toRotationMatrix(view);
		
        Matrix4f proj = new Matrix4f();
        proj.set(view.multLocal(orthoProjection).multLocal(biasMatrix)).transposeLocal();
        fowTexture.setMatrix(proj);
    }
	
    private void updateTexture() {
        init(DisplaySystem.getDisplaySystem().getRenderer());
		
        idBuff.clear();
        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D, idBuff);
        int oldTex = idBuff.get();
        
        imageData.rewind();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fowTexture.getTextureId());
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_LUMINANCE8, fowSize, fowSize, 0, GL11.GL_LUMINANCE,
                          GL11.GL_UNSIGNED_BYTE, imageData);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
    }
	
    private void setOrthoMatrix(Matrix4f result) {
        float left = -(fowSize/2) * scale;
        float right = (fowSize/2) * scale;
        float top = -(fowSize/2) * scale;
        float bottom = (fowSize/2) * scale;
        float far = 300;
        float near = 1;
        
        float tx = -((right+left) / (right-left));
        float ty = -((top+bottom) / (top-bottom));
        float tz = -((far+near) / (far-near));
        float xf = 2 / (right - left);
        float yf = 2 / (top - bottom);
        float zf = -2 / (far - near);
        
        result.set(new float[][] {
                {xf, 0 , 0 , tx},
                {0 , yf, 0 , ty},
                {0 , 0 , zf, tz},
                {0 , 0 , 0 , 1 }
            });
        
        result.transposeLocal();
    }
	
    /**
     * @see com.jme.renderer.pass.Pass#doRender(com.jme.renderer.Renderer)
     */
    public void doRender(Renderer r) {
        init(r); 

        saveEnforcedStates();
		
        fowTexture.setEnvironmentalMapMode(Texture.EnvironmentalMapMode.EyeLinear);
        
        context.enforceState(fowTextureState);
        context.enforceState(noLights);
        context.enforceState(blendState);
		
        r.setPolygonOffset(0, -10);
        for (Spatial spat : spatials) {
            spat.onDraw(r);
        }
        r.clearPolygonOffset();
        r.renderQueue();
		
        fowTexture.setEnvironmentalMapMode(Texture.EnvironmentalMapMode.None);
		
        replaceEnforcedStates();
    }
}