/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is more or less the same as ScreenshotAppState but without the keyboard input
 * (because in a headless environment, there is no keyboard and trying to configure it caused
 * errors).
 *
 * @author Richard Tingle (aka richtea)
 *
 */
public class ScreenshotNoInputAppState extends AbstractAppState implements ActionListener, SceneProcessor {

    private static final Logger logger = Logger.getLogger(ScreenshotNoInputAppState.class.getName());
    private String filePath;
    private boolean capture = false;
    private boolean numbered = true;
    private Renderer renderer;
    private RenderManager rm;
    private ByteBuffer outBuf;
    private String shotName;
    private long shotIndex = 0;
    private int width, height;

    /**
     * ViewPort to which the SceneProcessor is attached
     */
    private ViewPort last;

    /**
     * Using this constructor, the screenshot files will be written sequentially to the system
     * default storage folder.
     */
    public ScreenshotNoInputAppState() {
        this(null);
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot.
     * Include the separator at the end of the path.
     * Use an empty string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the separator at the end of the path.
     */
    public ScreenshotNoInputAppState(String filePath) {
        this.filePath = filePath;
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot.
     * Include the separator at the end of the path.
     * Use an empty string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the separator at the end of the path.
     * @param fileName The name of the file to save the screenshot as.
     */
    public ScreenshotNoInputAppState(String filePath, String fileName) {
        this.filePath = filePath;
        this.shotName = fileName;
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot and
     * a base index for the shot index.
     * Include the separator at the end of the path.
     * Use an empty string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the separator at the end of the path.
     * @param shotIndex The base index for screenshots.  The first screenshot will have
     *     shotIndex + 1 appended, the next shotIndex + 2, and so on.
     */
    public ScreenshotNoInputAppState(String filePath, long shotIndex) {
        this.filePath = filePath;
        this.shotIndex = shotIndex;
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot and
     * a base index for the shot index.
     * Include the separator at the end of the path.
     * Use an empty string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the separator at the end of the path.
     * @param fileName The name of the file to save the screenshot as.
     * @param shotIndex The base index for screenshots.  The first screenshot will have
     *     shotIndex + 1 appended, the next shotIndex + 2, and so on.
     */
    public ScreenshotNoInputAppState(String filePath, String fileName, long shotIndex) {
        this.filePath = filePath;
        this.shotName = fileName;
        this.shotIndex = shotIndex;
    }

    /**
     * Set the file path to store the screenshot.
     * Include the separator at the end of the path.
     * Use an empty string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath File path to use to store the screenshot. Include the separator at the end of the path.
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Set the file name of the screenshot.
     * @param fileName File name to save the screenshot as.
     */
    public void setFileName(String fileName) {
        this.shotName = fileName;
    }

    /**
     * Sets the base index that will used for subsequent screenshots.
     *
     * @param index the desired base index
     */
    public void setShotIndex(long index) {
        this.shotIndex = index;
    }

    /**
     * Sets if the filename should be appended with a number representing the
     * current sequence.
     * @param numberedWanted If numbering is wanted.
     */
    public void setIsNumbered(boolean numberedWanted) {
        this.numbered = numberedWanted;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        if (!super.isInitialized()) {
            List<ViewPort> vps = app.getRenderManager().getPostViews();
            last = vps.get(vps.size() - 1);
            last.addProcessor(this);

            if (shotName == null) {
                shotName = app.getClass().getSimpleName();
            }
        }

        super.initialize(stateManager, app);
    }

    /**
     * Clean up this AppState during the first update after it gets detached.
     * <p>
     * Because each ScreenshotAppState is also a SceneProcessor (in addition to
     * being an AppState) this method is also invoked when the SceneProcessor
     * get removed from its ViewPort, leading to an indirect recursion:
     * <ol><li>AppStateManager invokes ScreenshotAppState.cleanup()</li>
     * <li>cleanup() invokes ViewPort.removeProcessor()</li>
     * <li>removeProcessor() invokes ScreenshotAppState.cleanup()</li>
     * <li>... and so on.</li>
     * </ol>
     * <p>
     * In order to break this recursion, this method only removes the
     * SceneProcessor if it has not previously been removed.
     * <p>
     * A better design would have the AppState and SceneProcessor be 2 distinct
     * objects, but doing so now might break applications that rely on them
     * being a single object.
     */
    @Override
    public void cleanup() {
        ViewPort viewPort = last;
        if (viewPort != null) {
            last = null;
            viewPort.removeProcessor(this); // XXX indirect recursion!
        }

        super.cleanup();
    }

    @Override
    public void onAction(String name, boolean value, float tpf) {
        if (value) {
            capture = true;
        }
    }

    public void takeScreenshot() {
        capture = true;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        renderer = rm.getRenderer();
        this.rm = rm;
        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    @Override
    public boolean isInitialized() {
        return super.isInitialized() && renderer != null;
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        outBuf = BufferUtils.createByteBuffer(w * h * 4);
        width = w;
        height = h;
    }

    @Override
    public void preFrame(float tpf) {
        // do nothing
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // do nothing
    }

    @Override
    public void postFrame(FrameBuffer out) {
        if (capture) {
            capture = false;

            Camera curCamera = rm.getCurrentCamera();
            int viewX = (int) (curCamera.getViewPortLeft() * curCamera.getWidth());
            int viewY = (int) (curCamera.getViewPortBottom() * curCamera.getHeight());
            int viewWidth = (int) ((curCamera.getViewPortRight() - curCamera.getViewPortLeft()) * curCamera.getWidth());
            int viewHeight = (int) ((curCamera.getViewPortTop() - curCamera.getViewPortBottom()) * curCamera.getHeight());

            renderer.setViewPort(0, 0, width, height);
            renderer.readFrameBuffer(out, outBuf);
            renderer.setViewPort(viewX, viewY, viewWidth, viewHeight);

            File file;
            String filename;
            if (numbered) {
                shotIndex++;
                filename = shotName + shotIndex;
            } else {
                filename = shotName;
            }

            if (filePath == null) {
                file = new File(JmeSystem.getStorageFolder() + File.separator + filename + ".png").getAbsoluteFile();
            } else {
                file = new File(filePath + filename + ".png").getAbsoluteFile();
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Saving ScreenShot to: {0}", file.getAbsolutePath());
            }

            try {
                writeImageFile(file);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error while saving screenshot", ex);
            }
        }
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        // not implemented
    }

    /**
     * Called by postFrame() once the screen has been captured to outBuf.
     *
     * @param file the output file
     * @throws IOException if an I/O error occurs
     */
    protected void writeImageFile(File file) throws IOException {
        OutputStream outStream = new FileOutputStream(file);
        try {
            JmeSystem.writeImageFile(outStream, "png", outBuf, width, height);
        } finally {
            outStream.close();
        }
    }
}
