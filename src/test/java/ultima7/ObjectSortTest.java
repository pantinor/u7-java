package ultima7;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.mock.audio.MockAudio;
import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;
import com.badlogic.gdx.graphics.GL20;
import java.util.ArrayList;
import java.util.Collections;
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

        Chunk chunk2 = REGIONS[11][8].chunks[2][0];
        //Collections.sort(chunk2.objects);

        ObjectEntry t1 = get(chunk2.objects, 962, 7, 3, 5);
        ObjectEntry t2 = get(chunk2.objects, 291, 15, 9, 2);

        //ObjectRendering.compare(t1, t2);
        if (true) {
            //return;
        }

        Chunk chunk = REGIONS[8][4].chunks[8][2];

        ObjectEntry body = get(chunk.objects, 414, 10, 6, 0);
        ObjectEntry w1 = get(chunk.objects, 497, 8, 4, 0);

        for (ObjectEntry e : body.dependents) {
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
