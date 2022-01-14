package ultima7;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public abstract class BaseScreen implements Screen, InputProcessor {

    protected Stage stage;

    protected float time = 0;

    public final Vector3 newMapPixelCoords = new Vector3();

    protected final Viewport viewport = new ScreenViewport();

    protected OrthographicCamera camera;

    protected final Vector2 currentMousePos = new Vector2();

    protected int currentRoomId = 0;
    protected String roomName = null;

    /**
     * translate map tile coords to world pixel coords
     * @param v
     * @param x
     * @param y
     */
    public abstract void setMapPixelCoords(Vector3 v, int x, int y);

    /**
     * get the map coords at the camera center
     * @param v
     */
    public abstract void setCurrentMapCoords(Vector3 v);
    
    public abstract void log(String t);
    
    public int currentRoomId() {
        return this.currentRoomId;
    }

    @Override
    public void dispose() {

    }

    public Stage getStage() {
        return stage;
    }

    public abstract void finishTurn(int currentX, int currentY);


    @Override
    public void hide() {
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        currentMousePos.set(screenX, screenY);
        return false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

}
