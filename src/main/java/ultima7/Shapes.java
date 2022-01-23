/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ultima7;

/**
 *
 * @author panti
 */
public class Shapes {

    public static final int PATH = 607;
    public static final int BARGE = 961;
    public static final int[] SKIP_IDS = new int[]{PATH, BARGE};
    public static final int[] ROOF_IDS = new int[]{156, 161, 162, 164, 165, 166, 167, 169, 170, 171, 172, 173, 174, 175, 176, 223, 394, 908, 954, 956, 962, 963, 966};
    public static final int[] MOUNTAIN_IDS = new int[]{180, 182, 183, 324, 969, 983};

    public static boolean isRoof(int sid) {
        for (int r : ROOF_IDS) {
            if (r == sid) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMountain(int sid) {
        for (int r : MOUNTAIN_IDS) {
            if (r == sid) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSkip(int sid) {
        for (int r : ROOF_IDS) {
            if (r == sid) {
                return true;
            }
        }
        for (int r : MOUNTAIN_IDS) {
            if (r == sid) {
                return true;
            }
        }
        for (int r : SKIP_IDS) {
            if (r == sid) {
                return true;
            }
        }
        return false;
    }

    // TFA Shape Classes 
    public static final int unusable = 0;// Trees.
    public static final int quality = 2;
    public static final int quantity = 3;// Can have more than 1:  coins, arrs.
    public static final int has_hp = 4;// Breakable items (if hp != 0, that is)
    public static final int quality_flags = 5;// Item quality is set of flags:
    public static final int container = 6;
    public static final int hatchable = 7;// Eggs, traps, moongates.
    public static final int spellbook = 8;
    public static final int barge = 9;
    public static final int virtue_stone = 11;
    public static final int monster = 12;// Non-human's.
    public static final int human = 13;// Human NPC's.
    public static final int building = 14;// Roof, window, mountain.

}
