/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ultima7;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 *
 * @author panti
 */
public class Ultima7 extends Game {

    public static final int SCREEN_WIDTH = 1088;
    public static final int SCREEN_HEIGHT = 912;

    public static final int MAP_VIEWPORT_DIM = 768;
    public static Texture backGround;

    public static Ultima7 mainGame;
    public static Skin skin;

    public static BitmapFont font;
    public static BitmapFont smallFont;
    public static BitmapFont largeFont;
    public static BitmapFont hudLogFont;
    public static BitmapFont titleFont;
    
    public static TextureRegion AVATAR_TEXTURE;

    public static void main(String[] args) {

        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Ultima7";
        cfg.width = SCREEN_WIDTH;
        cfg.height = SCREEN_HEIGHT;
        cfg.addIcon("data/ankh.png", Files.FileType.Classpath);
        new LwjglApplication(new Ultima7(), cfg);

    }

    @Override
    public void create() {

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.classpath("fonts/gnuolane.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 16;
        hudLogFont = generator.generateFont(parameter);

        parameter.size = 18;
        font = generator.generateFont(parameter);
        font.setColor(Color.BLACK);

        parameter.size = 24;
        largeFont = generator.generateFont(parameter);

        parameter.size = 72;
        titleFont = generator.generateFont(parameter);

        generator.dispose();

        skin = new Skin(Gdx.files.classpath("skin/uiskin.json"));

        skin.remove("default-font", BitmapFont.class);
        skin.add("default-font", font, BitmapFont.class);
        skin.add("larger-font", largeFont, BitmapFont.class);
        skin.add("title-font", titleFont, BitmapFont.class);

        smallFont = skin.get("verdana-10", BitmapFont.class);
        skin.add("small-font", smallFont, BitmapFont.class);

        Label.LabelStyle ls = skin.get("default", Label.LabelStyle.class);
        ls.font = font;
        Label.LabelStyle ls2 = skin.get("hudLogFont", Label.LabelStyle.class);
        ls2.font = hudLogFont;
        Label.LabelStyle ls3 = skin.get("hudSmallFont", Label.LabelStyle.class);
        ls3.font = smallFont;

        TextButton.TextButtonStyle tbs = skin.get("default", TextButton.TextButtonStyle.class);
        tbs.font = font;
        TextButton.TextButtonStyle tbsred = skin.get("red", TextButton.TextButtonStyle.class);
        tbsred.font = font;
        TextButton.TextButtonStyle tbsbr = skin.get("brown", TextButton.TextButtonStyle.class);
        tbsbr.font = font;

        SelectBox.SelectBoxStyle sbs = skin.get("default", SelectBox.SelectBoxStyle.class);
        sbs.font = font;
        sbs.listStyle.font = font;

        CheckBox.CheckBoxStyle cbs = skin.get("default", CheckBox.CheckBoxStyle.class);
        cbs.font = font;

        List.ListStyle lis = skin.get("default", List.ListStyle.class);
        lis.font = font;

        TextField.TextFieldStyle tfs = skin.get("default", TextField.TextFieldStyle.class);
        tfs.font = font;

        try {

            mainGame = this;

            backGround = new Texture(Gdx.files.classpath("data/frame.png"));

            Constants.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finished loading");
        
        setScreen(new GameScreen());
    }

}
