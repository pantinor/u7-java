package ultima7;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.mock.audio.MockAudio;
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;
import com.badlogic.gdx.graphics.GL20;
import java.util.List;
import org.testng.annotations.Test;
import ultima7.Constants.ObjectEntry;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import ultima7.Constants.Chunk;
import static ultima7.Constants.REGIONS;

public class ObjectSortTest {

    @Test
    public void testSort() throws Exception {

        Gdx.gl = mock(GL20.class);
        Gdx.audio = new MockAudio();
        Gdx.graphics = new MockGraphics();

        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        Ultima7 u7 = new Ultima7();
        TestScreen sc = new TestScreen();
        u7.testScreen = sc;
        new HeadlessApplication(u7, cfg);

        while (!sc.ready) {
            Thread.sleep(500);
        }

        Chunk chunk = REGIONS[8][4].chunks[8][2];
        ObjectEntry body = get(chunk.objects, 414, 10, 6, 0);
        ObjectEntry w1 = get(chunk.objects, 497, 8, 4, 0);
        
        for (ObjectEntry e : chunk.objects) {
            System.out.println(Shapes.SHAPE_NAMES.get(e.shapeIndex) + " " + e.toString());
        }
        
        System.out.println("***");

        for (ObjectEntry e : body.dependents) {
            System.out.println(Shapes.SHAPE_NAMES.get(e.shapeIndex) + " " + e.toString());
        }
        
        System.out.println("***");

        for (ObjectEntry e : w1.dependents) {
            System.out.println(Shapes.SHAPE_NAMES.get(e.shapeIndex) + " " + e.toString());
        }

        int order = ObjectRendering.compare(body, w1);

        assertEquals(order, 1);

    }

    private ObjectEntry get(List<ObjectEntry> objects, int id, int tx, int ty, int tz) {
        for (ObjectEntry e : objects) {
            if (e.shapeIndex == id && e.tx == tx && e.ty == ty && e.tz == tz) {
                return e;
            }
        }
        return null;
    }

    private class TestScreen implements Screen {

        boolean ready = false;

        @Override
        public void show() {
            ready = true;
        }

        @Override
        public void render(float delta) {

        }

        @Override
        public void resize(int width, int height) {

        }

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void hide() {

        }

        @Override
        public void dispose() {

        }

    }

}
