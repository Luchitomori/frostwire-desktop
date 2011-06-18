package com.limegroup.gnutella.gui.themes.setters;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;

import org.limewire.util.OSUtils;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.fonts.SubstanceFontUtilities;
import org.pushingpixels.substance.internal.ui.SubstanceCheckBoxMenuItemUI;
import org.pushingpixels.substance.internal.ui.SubstanceMenuBarUI;
import org.pushingpixels.substance.internal.ui.SubstanceMenuItemUI;
import org.pushingpixels.substance.internal.ui.SubstanceMenuUI;
import org.pushingpixels.substance.internal.ui.SubstancePopupMenuSeparatorUI;
import org.pushingpixels.substance.internal.ui.SubstancePopupMenuUI;
import org.pushingpixels.substance.internal.ui.SubstanceRadioButtonMenuItemUI;
import org.pushingpixels.substance.internal.ui.SubstanceTableUI;
import org.pushingpixels.substance.internal.ui.SubstanceTreeUI;
import org.pushingpixels.substance.internal.utils.SubstanceCoreUtilities;

import com.frostwire.gui.components.RangeSlider;
import com.limegroup.gnutella.gui.themes.SkinComboBoxUI;
import com.limegroup.gnutella.gui.themes.SkinListUI;
import com.limegroup.gnutella.gui.themes.SkinRangeSliderUI;
import com.limegroup.gnutella.gui.themes.SkinTextAreaUI;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeSetter;

public class SubstanceThemeSetter implements ThemeSetter {

    private final String _name;
    private final String _skinClassName;
    
    private final float LINUX_SCALED_FONT_POLICY_FACTOR = 0.87f;
    private final float WINDOWS_SCALED_FONT_POLICY_FACTOR = 0.87f;
    private final float MAC_SCALED_FONT_POLICY_FACTOR = 0.87f;

    private SubstanceThemeSetter(String name, String skinClassName) {
        _name = name;
        _skinClassName = skinClassName;
    }

    public String getName() {
        return _name;
    }

    public void apply() {
        SubstanceLookAndFeel.setSkin(_skinClassName);
        ThemeMediator.applyCommonSkinUI();
        
        float scaledFontPolicyFactor = WINDOWS_SCALED_FONT_POLICY_FACTOR;
        if (OSUtils.isMacOSX()) {
        	scaledFontPolicyFactor = MAC_SCALED_FONT_POLICY_FACTOR;
        } else if (OSUtils.isLinux()) {
        	scaledFontPolicyFactor = LINUX_SCALED_FONT_POLICY_FACTOR;
        }
        
        SubstanceLookAndFeel.setFontPolicy(SubstanceFontUtilities.getScaledFontPolicy(scaledFontPolicyFactor));
        
        //reduceFont("Label.font");
        //reduceFont("Table.font");
        //ResourceManager.setFontSizes(-1);
        //ResourceManager.setFontSizes(0);

        UIManager.put("Tree.leafIcon", UIManager.getIcon("Tree.closedIcon"));

        // remove split pane borders
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());

        if (!OSUtils.isMacOSX()) {
            UIManager.put("Table.focusRowHighlightBorder", UIManager.get("Table.focusCellHighlightBorder"));
        }

        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // Add a bold text version of simple text.
        Font normal = UIManager.getFont("Table.font");
        FontUIResource bold = new FontUIResource(normal.getName(), Font.BOLD, normal.getSize());
        UIManager.put("Table.font.bold", bold);
        UIManager.put("Tree.rowHeight", 0);
    }

    // from FrostWire
    public static final SubstanceThemeSetter SEA_GLASS = new SubstanceThemeSetter("Sea Glass", "com.limegroup.gnutella.gui.themes.SeaGlassSkin");

    // from Substance
    public static final SubstanceThemeSetter AUTUMN = new SubstanceThemeSetter("Autumn", "org.pushingpixels.substance.api.skin.AutumnSkin");
    public static final SubstanceThemeSetter BUSINESS_BLACK_STEEL = new SubstanceThemeSetter("Business Black Steel", "org.pushingpixels.substance.api.skin.BusinessBlackSteelSkin");
    public static final SubstanceThemeSetter BUSINESS_BLUE_STEEL = new SubstanceThemeSetter("Business Blue Steel", "org.pushingpixels.substance.api.skin.BusinessBlueSteelSkin");
    public static final SubstanceThemeSetter BUSINESS = new SubstanceThemeSetter("Business", "org.pushingpixels.substance.api.skin.BusinessSkin");
    public static final SubstanceThemeSetter CHALLENGER_DEEP = new SubstanceThemeSetter("Challenger Deep", "org.pushingpixels.substance.api.skin.ChallengerDeepSkin");
    public static final SubstanceThemeSetter CREME_COFFEE = new SubstanceThemeSetter("Creme Coffee", "org.pushingpixels.substance.api.skin.CremeCoffeeSkin");
    public static final SubstanceThemeSetter CREME = new SubstanceThemeSetter("Creme", "org.pushingpixels.substance.api.skin.CremeSkin");
    public static final SubstanceThemeSetter DUST_COFFEE = new SubstanceThemeSetter("Dust Coffee", "org.pushingpixels.substance.api.skin.DustCoffeeSkin");
    public static final SubstanceThemeSetter DUST = new SubstanceThemeSetter("Dust", "org.pushingpixels.substance.api.skin.DustSkin");
    public static final SubstanceThemeSetter EMERALD_DUSK = new SubstanceThemeSetter("Emerald Dusk", "org.pushingpixels.substance.api.skin.EmeraldDuskSkin");
    public static final SubstanceThemeSetter GEMINI = new SubstanceThemeSetter("Gemini", "org.pushingpixels.substance.api.skin.GeminiSkin");
    public static final SubstanceThemeSetter GRAPHITE_AQUA = new SubstanceThemeSetter("Graphite Aqua", "org.pushingpixels.substance.api.skin.GraphiteAquaSkin");
    public static final SubstanceThemeSetter GRAPHITE_GLASS = new SubstanceThemeSetter("Graphite Glass", "org.pushingpixels.substance.api.skin.GraphiteGlassSkin");
    public static final SubstanceThemeSetter GRAPHITE = new SubstanceThemeSetter("Graphite", "org.pushingpixels.substance.api.skin.GraphiteSkin");
    public static final SubstanceThemeSetter MAGELLAN = new SubstanceThemeSetter("Magellan", "org.pushingpixels.substance.api.skin.MagellanSkin");
    public static final SubstanceThemeSetter MARINER = new SubstanceThemeSetter("Mariner", "org.pushingpixels.substance.api.skin.MarinerSkin");
    public static final SubstanceThemeSetter MIST_AQUA = new SubstanceThemeSetter("Mist Aqua", "org.pushingpixels.substance.api.skin.MistAquaSkin");
    public static final SubstanceThemeSetter MIST_SILVER = new SubstanceThemeSetter("Mist Silver", "org.pushingpixels.substance.api.skin.MistSilverSkin");
    public static final SubstanceThemeSetter MODERATE = new SubstanceThemeSetter("Moderate", "org.pushingpixels.substance.api.skin.ModerateSkin");
    public static final SubstanceThemeSetter NEBULA_BRICK_WALL = new SubstanceThemeSetter("Nebula Brick Wall", "org.pushingpixels.substance.api.skin.NebulaBrickWallSkin");
    public static final SubstanceThemeSetter NEBULA = new SubstanceThemeSetter("Nebula", "org.pushingpixels.substance.api.skin.NebulaSkin");
    public static final SubstanceThemeSetter OFFICE_BLACK_2007 = new SubstanceThemeSetter("Office Black 2007", "org.pushingpixels.substance.api.skin.OfficeBlack2007Skin");
    public static final SubstanceThemeSetter OFFICE_BLUE_2007 = new SubstanceThemeSetter("Office Blue 2007", "org.pushingpixels.substance.api.skin.OfficeBlue2007Skin");
    public static final SubstanceThemeSetter OFFICE_SILVER_2007 = new SubstanceThemeSetter("Office Silver 2007", "org.pushingpixels.substance.api.skin.OfficeSilver2007Skin");
    public static final SubstanceThemeSetter RAVEN = new SubstanceThemeSetter("Raven", "org.pushingpixels.substance.api.skin.RavenSkin");
    public static final SubstanceThemeSetter SAHARA = new SubstanceThemeSetter("Sahara", "org.pushingpixels.substance.api.skin.SaharaSkin");
    public static final SubstanceThemeSetter TWILIGHT = new SubstanceThemeSetter("Twilight", "org.pushingpixels.substance.api.skin.TwilightSkin");

    // from Substance extras
    public static final SubstanceThemeSetter FIELD_OF_WHEAT = new SubstanceThemeSetter("Field Of Wheat", "org.pushingpixels.substance.skinpack.FieldOfWheatSkin");
    public static final SubstanceThemeSetter FINDING_NEMO = new SubstanceThemeSetter("Finding Nemo", "org.pushingpixels.substance.skinpack.FindingNemoSkin");
    public static final SubstanceThemeSetter GREEN_MAGIC = new SubstanceThemeSetter("Green Magic", "org.pushingpixels.substance.skinpack.GreenMagicSkin");
    public static final SubstanceThemeSetter MAGMA = new SubstanceThemeSetter("Magma", "org.pushingpixels.substance.skinpack.MagmaSkin");
    public static final SubstanceThemeSetter MANGO = new SubstanceThemeSetter("Mango", "org.pushingpixels.substance.skinpack.MangoSkin");
    public static final SubstanceThemeSetter STREETLIGHTS = new SubstanceThemeSetter("Streetlights", "org.pushingpixels.substance.skinpack.StreetlightsSkin");

    public ComponentUI createCheckBoxMenuItemUI(JComponent comp) {
        return SubstanceCheckBoxMenuItemUI.createUI(comp);
    }

    public ComponentUI createMenuBarUI(JComponent comp) {
        return SubstanceMenuBarUI.createUI(comp);
    }

    public ComponentUI createMenuItemUI(JComponent comp) {
        return SubstanceMenuItemUI.createUI(comp);
    }

    public ComponentUI createMenuUI(JComponent comp) {
        return SubstanceMenuUI.createUI(comp);
    }

    public ComponentUI createPopupMenuSeparatorUI(JComponent comp) {
        return SubstancePopupMenuSeparatorUI.createUI(comp);
    }

    public ComponentUI createPopupMenuUI(JComponent comp) {
        return SubstancePopupMenuUI.createUI(comp);
    }

    public ComponentUI createRadioButtonMenuItemUI(JComponent comp) {
        return SubstanceRadioButtonMenuItemUI.createUI(comp);
    }

    public ComponentUI createTextAreaUI(JComponent comp) {
        SubstanceCoreUtilities.testComponentCreationThreadingViolation(comp);
        return new SkinTextAreaUI(comp);
    }

    public ComponentUI createListUI(JComponent comp) {
        SubstanceCoreUtilities.testComponentCreationThreadingViolation(comp);
        return new SkinListUI();
    }

    public ComponentUI createComboBoxUI(JComponent comp) {
        SubstanceCoreUtilities.testComponentCreationThreadingViolation(comp);
        return new SkinComboBoxUI((JComboBox) comp);
    }

    public ComponentUI createTreeUI(JComponent comp) {
        return SubstanceTreeUI.createUI(comp);
    }

    public ComponentUI createTableUI(JComponent comp) {
        return SubstanceTableUI.createUI(comp);
    }

    public ComponentUI createRangeSliderUI(JComponent comp) {
        SubstanceCoreUtilities.testComponentCreationThreadingViolation(comp);
        return new SkinRangeSliderUI((RangeSlider) comp);
    }
}
