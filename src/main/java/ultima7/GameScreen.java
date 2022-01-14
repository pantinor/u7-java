package ultima7;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ultima7.Constants.Direction;
import static ultima7.Constants.TILE_DIM;

public class GameScreen extends BaseScreen {

    private final U7MapRenderer renderer;
    private final Batch batch;
    private final Viewport mapViewPort;
    private final Stage mapStage;

    public GameScreen() {

        batch = new SpriteBatch();

        stage = new Stage(viewport);

        camera = new OrthographicCamera(Ultima7.MAP_VIEWPORT_DIM, Ultima7.MAP_VIEWPORT_DIM);

        mapViewPort = new ScreenViewport(camera);

        mapStage = new Stage(mapViewPort);

        renderer = new U7MapRenderer(Constants.REGIONS, 1.0f);

        SequenceAction seq1 = Actions.action(SequenceAction.class);
        seq1.addAction(Actions.delay(5f));
        seq1.addAction(Actions.run(new GameTimer()));
        stage.addAction(Actions.forever(seq1));

        setMapPixelCoords(this.newMapPixelCoords, 232, 226);//1856, 1808
        //setMapPixelCoords(this.newMapPixelCoords, 200, 434);//1600, 3472

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
        v.set(tilex * TILE_DIM, Constants.MAP_HEIGHT - tiley * TILE_DIM, 0);
    }

    @Override
    public void setCurrentMapCoords(Vector3 v) {
        float dx = TILE_DIM * 32;
        float dy = TILE_DIM * 2308;
        Vector3 tmp = camera.unproject(new Vector3(dx, dy, 0), 0, 0, Ultima7.MAP_VIEWPORT_DIM, Ultima7.MAP_VIEWPORT_DIM);
        v.set(Math.round(tmp.x / TILE_DIM), ((Constants.MAP_HEIGHT - Math.round(tmp.y) - TILE_DIM) / TILE_DIM), 0);
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
                newMapPixelCoords.x + TILE_DIM * 16,
                newMapPixelCoords.y + TILE_DIM * 2688,
                0);

        camera.update();

        renderer.setView(camera.combined,
                camera.position.x - TILE_DIM * 64,
                camera.position.y - TILE_DIM * 48,
                Ultima7.MAP_VIEWPORT_DIM - TILE_DIM,
                Ultima7.MAP_VIEWPORT_DIM - TILE_DIM);

        renderer.render();

        batch.begin();

        batch.draw(Ultima7.backGround, 0, 0);
        batch.draw(Constants.RECORDS.get(64).frames[0].texture, TILE_DIM * 52, TILE_DIM * 56, TILE_DIM, TILE_DIM);

        Vector3 v = new Vector3();
        setCurrentMapCoords(v);
        Ultima7.font.draw(batch, String.format("[%.0f, %.0f]", v.x, v.y), 200, Ultima7.SCREEN_HEIGHT - 24);

        batch.end();

        mapStage.act();
        mapStage.draw();

        stage.act();
        stage.draw();

    }

    @Override
    public boolean keyUp(int keycode) {
        Vector3 v = new Vector3();
        setCurrentMapCoords(v);

        if (keycode == Keys.UP) {

            if (!preMove(v, Direction.NORTH)) {
                return false;
            }
            newMapPixelCoords.y = newMapPixelCoords.y + TILE_DIM;
            v.y -= 1;
        } else if (keycode == Keys.DOWN) {

            if (!preMove(v, Direction.SOUTH)) {
                return false;
            }
            newMapPixelCoords.y = newMapPixelCoords.y - TILE_DIM;
            v.y += 1;
        } else if (keycode == Keys.RIGHT) {

            if (!preMove(v, Direction.EAST)) {
                return false;
            }
            newMapPixelCoords.x = newMapPixelCoords.x + TILE_DIM;
            v.x += 1;
        } else if (keycode == Keys.LEFT) {

            if (!preMove(v, Direction.WEST)) {
                return false;
            }
            newMapPixelCoords.x = newMapPixelCoords.x - TILE_DIM;
            v.x -= 1;
        } else if (keycode == Keys.U) {

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

        //if (nx > this.map.getWidth() - 1 || nx < 0 || ny > this.map.getHeight() - 1 || ny < 0) {
        //Andius.mainGame.setScreen(Map.WORLD.getScreen());
        //return false;
        //}
//        TiledMapTileLayer layer = (TiledMapTileLayer) this.map.getTiledMap().getLayers().get("base");
//        TiledMapTileLayer.Cell cell = layer.getCell(nx, this.map.getHeight() - 1 - ny);
//        if (cell != null) {
//            TileFlags tf = Ultima6.TILE_FLAGS.get(cell.getTile().getId() - 1);
//            if (tf.isWall() || tf.isImpassable() || tf.isWet()) {
//                TileFlags otf = null;
//                MapLayer objLayer = this.map.getTiledMap().getLayers().get("objects");
//                for (MapObject obj : objLayer.getObjects()) {
//                    TiledMapTileMapObject tmo = (TiledMapTileMapObject) obj;
//                    float ox = ((Float) tmo.getProperties().get("x")) / 16;
//                    float oy = ((Float) tmo.getProperties().get("y")) / 16;
//                    if (ox == nx && oy == this.map.getHeight() - 1 - ny && !tf.isWall()) {
//                        int gid = (Integer) tmo.getProperties().get("gid");
//                        otf = Ultima6.TILE_FLAGS.get(gid - 1);
//                        break;
//                    }
//                }
//                if (otf != null) {
//                    if (otf.isWall() || otf.isImpassable() || otf.isWet()) {
//                        Sounds.play(Sound.BLOCKED);
//                        //return false;
//                    }
//                } else {
//                    Sounds.play(Sound.BLOCKED);
//                    //return false;
//                }
//            }
//        } else {
//            Sounds.play(Sound.BLOCKED);
//            //return false;
//        }
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
//                if (System.currentTimeMillis() - CLOCK.getLastIncrementTime() > 15 * 1000) {
//                    keyUp(Keys.SPACE);
//                }
            }
        }
    }
}
