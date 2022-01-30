package ultima7;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ultima7.Constants.Chunk;
import ultima7.Constants.Direction;
import static ultima7.Constants.MAP_TILE_HEIGHT;
import static ultima7.Constants.REGIONS;
import static ultima7.Constants.TILE_DIM;
import static ultima7.Ultima7.MAP_VIEWPORT_DIM;
import static ultima7.Ultima7.SCREEN_HEIGHT;

public class GameScreen extends BaseScreen {

    private final U7MapRenderer renderer;
    private final Batch batch;
    private final Viewport mapViewPort;
    private final Stage mapStage;

    private final float unitScale = 2.0f;

    Texture debugGridTexture = Constants.getTexture(Color.YELLOW, 1, 1);
    Texture debugPointerTexture = Constants.getTexture(Color.RED, TILE_DIM * 2, TILE_DIM * 2);

    private Direction movingDirection = null;

    public GameScreen() {
        batch = new SpriteBatch();

        stage = new Stage(viewport);

        camera = new OrthographicCamera(Ultima7.MAP_VIEWPORT_DIM, Ultima7.MAP_VIEWPORT_DIM);

        mapViewPort = new ScreenViewport(camera);

        mapStage = new Stage(mapViewPort);

        renderer = new U7MapRenderer(Constants.REGIONS, unitScale);

        SequenceAction seq1 = Actions.action(SequenceAction.class);
        seq1.addAction(Actions.delay(5f));
        seq1.addAction(Actions.run(new GameTimer()));
        stage.addAction(Actions.forever(seq1));

        SequenceAction seq2 = Actions.action(SequenceAction.class);
        seq2.addAction(Actions.delay(.1f));
        seq2.addAction(Actions.run(new AvatarMovementTimer()));
        stage.addAction(Actions.forever(seq2));

        //setMapPixelCoords(this.newMapPixelCoords, 1070, 2214);
        //setMapPixelCoords(this.newMapPixelCoords, 985, 2215);
        //setMapPixelCoords(this.newMapPixelCoords, 244, 245);
        setMapPixelCoords(this.newMapPixelCoords, 1069, 2195);

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(this, stage));
    }

    @Override
    public void hide() {
    }

    @Override
    public void setMapPixelCoords(Vector3 v, int tilex, int tiley) {
        v.set(tilex * TILE_DIM * unitScale, MAP_TILE_HEIGHT * unitScale - tiley * TILE_DIM * unitScale, 0);
    }

    @Override
    public void setCurrentMapCoords(Vector3 v) {
        float dx = TILE_DIM * 40;
        float dy = TILE_DIM * 4906;

        Vector3 tmp = camera.unproject(new Vector3(dx, dy, 0), 32, 80, MAP_VIEWPORT_DIM, MAP_VIEWPORT_DIM);
        v.set(Math.round(tmp.x / TILE_DIM), (MAP_TILE_HEIGHT - Math.round(tmp.y) - TILE_DIM) / TILE_DIM, 0);

        v.set(v.x / unitScale, v.y / unitScale, 0);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        mapViewPort.update(width, height, false);
    }

    @Override
    public void render(float delta) {

        time += delta;

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (renderer == null) {
            return;
        }

        camera.position.set(
                newMapPixelCoords.x + 128,
                newMapPixelCoords.y + 43000,
                0);

        camera.update();

        renderer.setView(camera.combined,
                camera.position.x - TILE_DIM * 64,
                camera.position.y - TILE_DIM * 48,
                MAP_VIEWPORT_DIM - TILE_DIM,
                MAP_VIEWPORT_DIM - TILE_DIM);

        renderer.render();

        batch.begin();

        batch.draw(Ultima7.backGround, 0, 0);

        batch.draw(debugPointerTexture, TILE_DIM * 52, TILE_DIM * 56);
        //batch.draw(Constants.RECORDS.get(721).frames[0].texture, TILE_DIM * 52, TILE_DIM * 56);

        //drawDebugGrid(debugGridTexture, batch, TILE_DIM * unitScale);
        Vector3 v = new Vector3();
        setCurrentMapCoords(v);
        int nx = (int) v.x;
        int ny = (int) v.y;
        int sx = nx * 8 / 2048;
        int sy = ny * 8 / 2048;
        int cx = ((nx * 8) - (sx * 2048)) / 128;
        int cy = ((ny * 8) - (sy * 2048)) / 128;
        Ultima7.font.draw(batch, String.format("[%d, %d] region [%d][%d] chunk [%d][%d]", nx, ny, sy, sx, cy, cx), 200, SCREEN_HEIGHT - 24);

        batch.end();

        mapStage.act();
        mapStage.draw();

        stage.act();
        stage.draw();

    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.UP) {
            this.movingDirection = Direction.NORTH;
        } else if (keycode == Keys.DOWN) {
            this.movingDirection = Direction.SOUTH;
        } else if (keycode == Keys.RIGHT) {
            this.movingDirection = Direction.EAST;
        } else if (keycode == Keys.LEFT) {
            this.movingDirection = Direction.WEST;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {

        this.movingDirection = null;

        Vector3 v = new Vector3();
        setCurrentMapCoords(v);

        if (keycode == Keys.U) {

        } else if (keycode == Keys.E || keycode == Keys.K) {

        } else if (keycode == Keys.G) {

        } else if (keycode == Keys.T) {

        }

        finishTurn((int) v.x, (int) v.y);

        return false;
    }

    private boolean preMove(Vector3 current, Direction dir) {

        int nx = (int) current.x;
        int ny = (int) current.y;

        if (dir == Direction.NORTH) {
            ny = (int) current.y - 1;
        }
        if (dir == Direction.SOUTH) {
            ny = (int) current.y + 1;
        }
        if (dir == Direction.WEST) {
            nx = (int) current.x - 1;
        }
        if (dir == Direction.EAST) {
            nx = (int) current.x + 1;
        }

        //TODO check if blocked here
        int sx = nx * 8 / 2048;
        int sy = ny * 8 / 2048;
        int cx = ((nx * 8) - (sx * 2048)) / 128;
        int cy = ((ny * 8) - (sy * 2048)) / 128;

        Chunk chunk = REGIONS[sy][sx].chunks[cy][cx];

        return true;
    }

    @Override
    public void finishTurn(int currentX, int currentY) {

    }

    @Override
    public void log(String s) {
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    public class GameTimer implements Runnable {

        public boolean active = true;

        @Override
        public void run() {
            if (active) {

            }
        }
    }

    public class AvatarMovementTimer implements Runnable {

        @Override
        public void run() {
            if (movingDirection != null) {

                Vector3 v = new Vector3();
                setCurrentMapCoords(v);

                if (movingDirection == Direction.NORTH) {
                    if (preMove(v, Direction.NORTH)) {
                        newMapPixelCoords.y = newMapPixelCoords.y + TILE_DIM * unitScale;
                        v.y -= 1;
                    }
                } else if (movingDirection == Direction.SOUTH) {
                    if (preMove(v, Direction.SOUTH)) {
                        newMapPixelCoords.y = newMapPixelCoords.y - TILE_DIM * unitScale;
                        v.y += 1;
                    }
                } else if (movingDirection == Direction.EAST) {
                    if (preMove(v, Direction.EAST)) {
                        newMapPixelCoords.x = newMapPixelCoords.x + TILE_DIM * unitScale;
                        v.x += 1;
                    }
                } else if (movingDirection == Direction.WEST) {
                    if (preMove(v, Direction.WEST)) {
                        newMapPixelCoords.x = newMapPixelCoords.x - TILE_DIM * unitScale;
                        v.x -= 1;
                    }
                }
            }
        }
    }
}
