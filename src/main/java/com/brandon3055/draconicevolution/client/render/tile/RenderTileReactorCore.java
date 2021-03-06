package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.OBJParser;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.shader.ShaderProgram;
import codechicken.lib.render.state.GlStateTracker;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.client.render.TESRBase;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.client.render.shaders.DEShaders;
import com.brandon3055.draconicevolution.helpers.ResourceHelperDE;
import com.brandon3055.draconicevolution.utils.DETextures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

import java.util.Map;

import static com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore.MAX_TEMPERATURE;

/**
 * Created by brandon3055 on 6/11/2016.
 */
public class RenderTileReactorCore extends TESRBase<TileReactorCore> {

    private static CCModel model = null;
    private static CCModel model_no_shade;

    private static ShaderProgram reactorProgram;
    private static ShaderProgram shieldProgram;

    public RenderTileReactorCore() {
        if (model == null) {
            Map<String, CCModel> map = OBJParser.parseModels(ResourceHelperDE.getResource("models/block/obj_models/reactor_core.obj"));
            model = CCModel.combine(map.values());
            map = OBJParser.parseModels(ResourceHelperDE.getResource("models/reactor_core_model.obj"));
            model_no_shade = CCModel.combine(map.values());
        }
    }

    @Override
    public void renderTileEntityAt(TileReactorCore te, double x, double y, double z, float partialTicks, int destroyStage) {
        GlStateManager.pushMatrix();
        GlStateTracker.pushState();
        GlStateManager.disableLighting();

        setLighting(200);
        double diameter = te.getCoreDiameter();
        float t = (float) (te.temperature.value / MAX_TEMPERATURE);
        float intensity = t <= 0.2 ? (float) Utils.map(t, 0, 0.2, 0, 0.3) : t <= 0.8 ? (float) Utils.map(t, 0.2, 0.8, 0.3, 1) : (float) Utils.map(t, 0.8, 1, 1, 1.3);
        float shieldPower = (float) (te.maxShieldCharge.value > 0 ? te.shieldCharge.value / te.maxShieldCharge.value : 0);

        if (MinecraftForgeClient.getRenderPass() == 0) {
            float animation = (te.coreAnimation + (partialTicks * (float) te.shaderAnimationState.value)) / 20F;
            renderCore(x, y, z, partialTicks, intensity, animation, diameter, DEShaders.useShaders());
        } else if (te.shieldAnimationState > 0) {
            float animation = (te.shieldAnimation + (partialTicks * te.shieldAnimationState)) / 20F;

            float power = (0.7F * shieldPower) - (1 - te.shieldAnimationState);
            if (te.reactorState.value == TileReactorCore.ReactorState.BEYOND_HOPE) {
                power = 0.05F;//0.05F + ((float) (Math.sin(ClientEventHandler.elapsedTicks / 5F) + 1) / 20F);
            }

            renderShield(x, y, z, partialTicks, power, animation, diameter, DEShaders.useShaders());
        }

        resetLighting();
        GlStateTracker.popState();
        GlStateManager.popMatrix();
    }

    public void renderItem() {
        GlStateManager.pushMatrix();
        GlStateTracker.pushState();
        GlStateManager.disableLighting();
        setLighting(200);
        float scale = 1.3F;
        float animation = 0;
        float intensity = 0;

        renderCore(0, 0, 0, 0, intensity, animation, scale, DEShaders.useShaders());

        resetLighting();
        GlStateTracker.popState();
        GlStateManager.popMatrix();
    }

    public static void renderGUI(TileReactorCore te, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateTracker.pushState();

        double diameter = 100;
        float t = (float) (te.temperature.value / MAX_TEMPERATURE);
        float intensity = t <= 0.2 ? (float) Utils.map(t, 0, 0.2, 0, 0.3) : t <= 0.8 ? (float) Utils.map(t, 0.2, 0.8, 0.3, 1) : (float) Utils.map(t, 0.8, 1, 1, 1.3);
        float animation = (te.coreAnimation + (0 * (float) te.shaderAnimationState.value)) / 20F;
        float shieldPower = (float) (te.maxShieldCharge.value > 0 ? te.shieldCharge.value / te.maxShieldCharge.value : 0);

        renderCore(x - 0.5, y, 100, 0, intensity, animation, diameter, DEShaders.useShaders());
        renderShield(x - 0.5, y, 100, 0, (0.7F * shieldPower) - (float) (1 - te.shaderAnimationState.value), animation, diameter, DEShaders.useShaders());

        GlStateTracker.popState();
        GlStateManager.popMatrix();
    }

    private static void renderCore(double x, double y, double z, float partialTicks, float intensity, float animation, double diameter, boolean useShader) {
        ResourceHelperDE.bindTexture(DETextures.REACTOR_CORE);
        if (useShader) {
            if (reactorProgram == null) {
                reactorProgram = new ShaderProgram();
                reactorProgram.attachShader(DEShaders.reactor);
            }
            reactorProgram.useShader(cache -> {
                cache.glUniform1F("time", animation);
                cache.glUniform1F("intensity", intensity);
            });
        }

        CCRenderState ccrs = CCRenderState.instance();
        ccrs.startDrawing(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);
        Matrix4 mat = RenderUtils.getMatrix(new Vector3(x + 0.5, y + 0.5, z + 0.5), new Rotation((ClientEventHandler.elapsedTicks + partialTicks) / 400F, 0, 1, 0), diameter);
        model.render(ccrs, mat);
        ccrs.draw();

        if (useShader) {
            reactorProgram.releaseShader();
        }
    }

    private static void renderShield(double x, double y, double z, float partialTicks, float power, float animation, double diameter, boolean useShader) {
        ResourceHelperDE.bindTexture(DETextures.REACTOR_SHIELD);
        if (useShader) {
            if (shieldProgram == null) {
                shieldProgram = new ShaderProgram();
                shieldProgram.attachShader(DEShaders.reactorShield);
            }
            shieldProgram.useShader(cache -> {
                cache.glUniform1F("time", animation);
                cache.glUniform1F("intensity", power);
            });
        } else {
            float ff = 0.5F;//tile.maxFieldCharge > 0 ? tile.fieldCharge / tile.maxFieldCharge : 0;
            float r = ff < 0.5F ? 1 - (ff * 2) : 0;
            float g = ff > 0.5F ? (ff - 0.5F) * 2 : 0;
            float b = ff * 2;
            float a = ff < 0.1F ? (ff * 10) : 1;
            GlStateManager.color(r, g, b, a);
        }

        GlStateManager.enableBlend();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.startDrawing(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX);

        if (useShader) {
            Matrix4 mat = RenderUtils.getMatrix(new Vector3(x + 0.5, y + 0.5, z + 0.5), new Rotation((ClientEventHandler.elapsedTicks + partialTicks) / 400F, 0, 1, 0), diameter * 1.05);
            model.render(ccrs, mat);
        } else {
            Matrix4 mat = RenderUtils.getMatrix(new Vector3(x + 0.5, y + 0.5, z + 0.5), new Rotation((ClientEventHandler.elapsedTicks + partialTicks) / 400F, 0, 1, 0), diameter * -0.525);
            model_no_shade.render(ccrs, mat);
        }

        ccrs.draw();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        if (useShader) {
            shieldProgram.releaseShader();
        }
    }

}
