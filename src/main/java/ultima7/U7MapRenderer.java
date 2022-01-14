package ultima7;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import static com.badlogic.gdx.graphics.g2d.Batch.C1;
import static com.badlogic.gdx.graphics.g2d.Batch.C2;
import static com.badlogic.gdx.graphics.g2d.Batch.C3;
import static com.badlogic.gdx.graphics.g2d.Batch.C4;
import static com.badlogic.gdx.graphics.g2d.Batch.U1;
import static com.badlogic.gdx.graphics.g2d.Batch.U2;
import static com.badlogic.gdx.graphics.g2d.Batch.U3;
import static com.badlogic.gdx.graphics.g2d.Batch.U4;
import static com.badlogic.gdx.graphics.g2d.Batch.V1;
import static com.badlogic.gdx.graphics.g2d.Batch.V2;
import static com.badlogic.gdx.graphics.g2d.Batch.V3;
import static com.badlogic.gdx.graphics.g2d.Batch.V4;
import static com.badlogic.gdx.graphics.g2d.Batch.X1;
import static com.badlogic.gdx.graphics.g2d.Batch.X2;
import static com.badlogic.gdx.graphics.g2d.Batch.X3;
import static com.badlogic.gdx.graphics.g2d.Batch.X4;
import static com.badlogic.gdx.graphics.g2d.Batch.Y1;
import static com.badlogic.gdx.graphics.g2d.Batch.Y2;
import static com.badlogic.gdx.graphics.g2d.Batch.Y3;
import static com.badlogic.gdx.graphics.g2d.Batch.Y4;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import ultima7.Constants.Chunk;
import static ultima7.Constants.MAP_HEIGHT;
import static ultima7.Constants.MAP_WIDTH;
import ultima7.Constants.ObjectEntry;
import static ultima7.Constants.RECORDS;
import ultima7.Constants.Record;
import ultima7.Constants.Region;
import ultima7.Constants.Shape;
import static ultima7.Constants.TILE_DIM;

public class U7MapRenderer implements MapRenderer, Disposable {

    static protected final int NUM_VERTICES = 20;

    protected Region[][] map;

    protected float unitScale;
    private float stateTime = 0;

    protected Batch batch;

    protected Rectangle viewBounds;
    protected Rectangle imageBounds = new Rectangle();

    protected boolean ownsBatch;

    protected float vertices[] = new float[NUM_VERTICES];
    private final List<Chunk> renderingChunks = new ArrayList<>(36);

    public Region[][] getMap() {
        return map;
    }

    public float getUnitScale() {
        return unitScale;
    }

    public Batch getBatch() {
        return batch;
    }

    public Rectangle getViewBounds() {
        return viewBounds;
    }

    public U7MapRenderer(Region[][] map) {
        this(map, 1.0f);
    }

    public U7MapRenderer(Region[][] map, float unitScale) {
        this.map = map;
        this.unitScale = unitScale;
        this.viewBounds = new Rectangle();
        this.batch = new SpriteBatch();
        this.ownsBatch = true;
    }

    @Override
    public void setView(OrthographicCamera camera) {
        batch.setProjectionMatrix(camera.combined);
        float width = camera.viewportWidth * camera.zoom;
        float height = camera.viewportHeight * camera.zoom;
        float w = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x);
        float h = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x);
        viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h);
    }

    @Override
    public void setView(Matrix4 projection, float x, float y, float width, float height) {
        batch.setProjectionMatrix(projection);
        viewBounds.set(x, y, width, height);
    }

    @Override
    public void render() {
        beginRender();

        this.stateTime += Gdx.graphics.getDeltaTime() / 4;

        final Color batchColor = batch.getColor();
        final float color = Color.toFloatBits(batchColor.r, batchColor.g, batchColor.b, batchColor.a * 1);

        int layerWidth = MAP_WIDTH;
        int layerHeight = MAP_HEIGHT;

        int layerTileWidth = (int) (TILE_DIM * unitScale);
        int layerTileHeight = (int) (TILE_DIM * unitScale);

        int col1 = Math.max(0, (int) (viewBounds.x / layerTileWidth));
        int col2 = Math.min(layerWidth, (int) ((viewBounds.x + viewBounds.width + layerTileWidth) / layerTileWidth));
        int row1 = Math.max(0, (int) (viewBounds.y / layerTileHeight));
        int row2 = Math.min(layerHeight, (int) ((viewBounds.y + viewBounds.height + layerTileHeight) / layerTileHeight));

        float y = row2 * layerTileHeight;
        float startX = col1 * layerTileWidth;

        for (int row = row2; row >= row1; row--) {

            float x = startX;
            for (int col = col1; col < col2; col++) {

                int sx = col / 256;
                int sy = row / 256;

                int scx = (col - sx * 256) / 16;
                int scy = (row - sy * 256) / 16;

                int cx = (col - sx * 256) - (scx * 16);
                int cy = (row - sy * 256) - (scy * 16);

                Region region = map[11 - sy][sx];
                Chunk chunk = region.chunks[15 - scy][scx];
                Shape shape = chunk.shapes[15 - cy][cx];
                Record rec = RECORDS.get(shape.shapeIndex);

                if (!renderingChunks.contains(chunk)) {
                    renderingChunks.add(chunk);
                }

                TextureRegion tr = rec.frames[shape.frameIndex].texture;

                if (tr.getRegionWidth() != 8 && tr.getRegionHeight() != 8) {
                    x += layerTileWidth;
                    continue;
                }

                batch.draw(tr.getTexture(), x, y);

                x += layerTileWidth;
            }
            y -= layerTileHeight;
        }

        y = row2 * layerTileHeight;
        startX = col1 * layerTileWidth;

        for (int row = row2; row >= row1; row--) {

            float x = startX;
            for (int col = col1; col < col2; col++) {

                int sx = col / 256;
                int sy = row / 256;

                int scx = (col - sx * 256) / 16;
                int scy = (row - sy * 256) / 16;

                int cx = (col - sx * 256) - (scx * 16);
                int cy = (row - sy * 256) - (scy * 16);

                Region region = map[11 - sy][sx];
                Chunk chunk = region.chunks[15 - scy][scx];
                Shape shape = chunk.shapes[15 - cy][cx];
                Record rec = RECORDS.get(shape.shapeIndex);

                TextureRegion tr = rec.frames[shape.frameIndex].texture;

                if (rec.isRawChunkBits()) {
                    x += layerTileWidth;
                    continue;
                }

                float tx = x - tr.getRegionWidth() + TILE_DIM;
                float ty = y;

                batch.draw(tr.getTexture(), tx, ty);

                x += layerTileWidth;
            }
            y -= layerTileHeight;
        }

        for (Chunk chunk : renderingChunks) {
            if (!chunk.objects.isEmpty()) {
                for (int i = chunk.objects.size() - 1; i >= 0; i--) {
                    ObjectEntry e = chunk.objects.get(i);
                    Record rec = RECORDS.get(e.shapeIndex);

                    //skip roofs etc above ground level
                    if (e.tz > 1) {
                        continue;
                    }

                    if (e.frameIndex >= rec.frames.length) {
                        continue;
                    }

                    TextureRegion tr = rec.frames[e.frameIndex].texture;

                    int lft = 4 * e.tz;

                    float rx = (chunk.sx * 2048) + (16 * TILE_DIM * chunk.cx) + (TILE_DIM * e.tx) - lft - 1;
                    float ry = (chunk.sy * 2048) + (16 * TILE_DIM * chunk.cy) + (TILE_DIM * e.ty) - lft - 1;

                    batch.draw(tr, rx, 2048 * 12 - ry);
                }
            }
        }

        renderingChunks.clear();

        endRender();
    }

    @Override
    public void render(int[] layers) {
        beginRender();

        endRender();
    }

    protected void beginRender() {
        batch.begin();
    }

    protected void endRender() {
        batch.end();
    }

    @Override
    public void dispose() {
        if (ownsBatch) {
            batch.dispose();
        }
    }

}
