package tonegod.gui.core;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import rlib.util.pools.Reusable;
import tonegod.gui.controls.extras.DragElement;
import tonegod.gui.controls.form.Form;
import tonegod.gui.core.layouts.Layout;
import tonegod.gui.core.layouts.LayoutHints;
import tonegod.gui.core.utils.UIDUtil;
import tonegod.gui.effects.Effect;
import tonegod.gui.style.Style;

/**
 * <p> The Element class is the primitive in which all controls in the GUI portion or the library
 * are built upon.  Unlink the overloaded common constructor(s) you will find throughout the
 * library, the library, there is a single verbose constructor:<br/> </br>
 *
 * @author t0neg0d
 * @see #Element(tonegod.gui.core.ElementManager, java.lang.String, com.jme3.math.Vector2f,
 * com.jme3.math.Vector2f, com.jme3.math.Vector4f, java.lang.String) </p> <p> Element is backed by a
 * 9-patch style Mesh and can be both movable and resizable by simply flagging them as such.  There
 * is no need to add Listeners to leverage this default behavior.<br/> <br/> See both:<br/>
 * @see #setIsMovable(boolean)
 * @see #setIsResizable(boolean) </p>
 */
public class Element extends Node implements Reusable {

    public static final String PARAM_COLOR_MAP = "ColorMap";
    public static final String PARAM_COLOR = "Color";
    public static final String PARAM_OFFSET_ALPHA_TEX_COORD = "OffsetAlphaTexCoord";
    public static final String PARAM_ALPHA_MAP = "AlphaMap";
    public static final String PARAM_USE_EFFECT_TEX_COORDS = "UseEffectTexCoords";
    public static final String PARAM_GLOBAL_ALPHA = "GlobalAlpha";

    public static enum Borders {
        NW,
        N,
        NE,
        W,
        E,
        SW,
        S,
        SE;
    }

    ;

    /**
     * Some controls provide different layout's based on the orientation of the control
     */
    public static enum Orientation {
        /**
         * Vertical layout
         */
        VERTICAL,
        /**
         * Horizontal layout
         */
        HORIZONTAL
    }

    /**
     * Defines how the element will dock to it's parent element during resize events
     */
    public static enum Docking {
        /**
         * Docks to the top left of parent
         */
        NW,
        /**
         * Docks to the top right of parent
         */
        NE,
        /**
         * Docks to the bottom left of parent
         */
        SW,
        /**
         * Docks to the bottom right of parent
         */
        SE
    }

    //<editor-fold desc="Fields">
    protected Application app;
    protected ElementManager screen;

    private String UID;

    public Vector4f borders = new Vector4f(1, 1, 1, 1);

    protected final Vector2f orgPosition;

    protected Vector4f borderHandles = new Vector4f(12, 12, 12, 12);
    protected Vector2f minDimensions = new Vector2f(10, 10);
    protected Vector2f position = new Vector2f();
    protected Vector2f dimensions = new Vector2f();
    protected Vector2f orgDimensions, orgRelDimensions;

    protected boolean isMovable = false;

    private boolean ignoreMouse = false;
    private boolean ignoreMouseLeftButton = false;
    private boolean ignoreMouseRightButton = false;
    private boolean ignoreMouseWheelClick = true;
    private boolean ignoreMouseWheelMove = true;
    private boolean ignoreMouseFocus = false;
    private boolean ignoreTouch = false;
    private boolean ignoreTouchMove = false;
    private boolean ignoreFling = false;
    private boolean lockToParentBounds = false;
    private boolean isResizable = false;
    private boolean resizeN = true;
    private boolean resizeS = true;
    private boolean resizeW = true;
    private boolean resizeE = true;
    private boolean dockN = false;
    private boolean dockW = true;
    private boolean dockE = false;
    private boolean dockS = true;
    private boolean scaleNS = true;
    private boolean scaleEW = true;
    private boolean effectParent = false;
    private boolean effectAbsoluteParent = false;
    private boolean useLocalAtlas = false;
    private boolean useLocalTexture = false;
    private boolean tileImage = false;

    private ElementQuadGrid model;
    private Geometry geom;
    private Material material;

    private String atlasCoords = "";

    private Texture defaultTexture;
    private Texture alphaMap;

    protected LineWrapMode textWrap = LineWrapMode.Word;
    protected BitmapFont.Align textAlign = BitmapFont.Align.Left;
    protected BitmapFont.VAlign textVAlign = BitmapFont.VAlign.Top;

    protected BitmapText textElement;
    protected Vector2f textPosition = new Vector2f(0, 0);

    protected String text = "";
    protected String toolTipText = null;

    protected BitmapFont font;
    protected Vector4f textPadding = new Vector4f(0, 0, 0, 0);
    protected ColorRGBA fontColor = ColorRGBA.White;
    private ColorRGBA defaultColor = new ColorRGBA(1, 1, 1, 0);

    private Element elementParent = null;

    protected Map<String, Element> elementChildren = new LinkedHashMap<>();

    // Clipping
    protected boolean isClipped = false;
    protected boolean wasClipped = false;
    protected Element clippingLayer, secondaryClippingLayer;
    protected Vector4f clippingBounds = new Vector4f();
    private Vector4f clipPadding = new Vector4f(0, 0, 0, 0);
    private Vector4f textClipPadding = new Vector4f(0, 0, 0, 0);

    // New Clipping
    protected List<ClippingDefine> clippingLayers = new ArrayList();
    private List<ClippingDefine> remClippingLayers = new ArrayList();
    private Vector4f clipTest = new Vector4f();

    protected boolean isVisible = true;
    protected boolean wasVisible = true;
    protected boolean isVisibleAsModal = false;
    private boolean hasFocus = false;
    private boolean resetKeyboardFocus = true;

    private Form form;
    private int tabIndex = 0;

    private float zOrder;
    private boolean effectZOrder = true;
    private Map<Effect.EffectEvent, Effect> effects = new HashMap();

    private OSRBridge bridge;

    private boolean ignoreGlobalAlpha = false;
    private boolean isModal = false;
    private boolean isGlobalModal = false;

    private Object elementUserData;

    private boolean initialized = false;
    protected boolean isEnabled = true;

    private boolean isDragElement = false, isDropElement = false;

    private Docking docking = Docking.NW;
    private Orientation orientation = Orientation.HORIZONTAL;

    protected float fontSize = 20;

    // Layouts
    protected Layout layout = null;
    protected LayoutHints layoutHints = new LayoutHints();
    //</editor-fold>

    /**
     * The Element class is the single primitive for all controls in the gui library. Each element
     * consists of an ElementQuadMesh for rendering resizable textures, as well as a BitmapText
     * element if setText(String text) is called.
     *
     * Behaviors, such as movement and resizing, are common to all elements and can be
     * enabled/disabled to ensure the element reacts to user input as needed.
     *
     * @param screen        The Screen control the element or it's absolute parent element is being
     *                      added to
     * @param UID           A unique String identifier used when looking up elements by
     *                      screen.getElementByID()
     * @param position      A Vector2f containing the x/y coordinates (relative to it's parent
     *                      elements x/y) for positioning
     * @param dimensions    A Vector2f containing the dimensions of the element, x being width, y
     *                      being height
     * @param resizeBorders A Vector4f containing the size of each border used for scaling images
     *                      without distorting them (x = N, y = W, x = E, w = S)
     * @param texturePath   A String path to the default image to be rendered on the element's mesh
     */
    public Element(ElementManager screen, String UID, Vector2f position, Vector2f dimensions, Vector4f resizeBorders, String texturePath) {
        this.app = screen.getApplication();
        this.screen = screen;

        if (UID == null) {
            this.UID = UIDUtil.getUID();
        } else {
            this.UID = UID;
        }

        this.orgPosition = new Vector2f();
        this.position.set(position);
        this.dimensions.set(dimensions);
        this.orgDimensions = dimensions.clone();
        this.orgRelDimensions = new Vector2f(1, 1);
        this.borders.set(resizeBorders);

        final AssetManager assetManager = app.getAssetManager();
        final Style styleFont = screen.getStyle("Font");

        final BitmapFont tempFont = assetManager.loadFont(styleFont.getString("defaultFont"));

        this.font = new BitmapFont();
        this.font.setCharSet(assetManager.loadFont(styleFont.getString("defaultFont")).getCharSet());

        final Material[] pages = new Material[tempFont.getPageSize()];

        for (int i = 0; i < pages.length; i++) {
            pages[i] = tempFont.getPage(i).clone();
        }

        this.font.setPages(pages);

        float imgWidth = 100;
        float imgHeight = 100;
        float pixelWidth = 1f / imgWidth;
        float pixelHeight = 1f / imgHeight;
        float textureAtlasX = 0, textureAtlasY = 0, textureAtlasW = imgWidth, textureAtlasH = imgHeight;

        boolean useAtlas = screen.getUseTextureAtlas();

        if (texturePath != null) {
            if (useAtlas && texturePath.contains("|") && texturePath.contains("=")) {

                float[] coords = screen.parseAtlasCoords(texturePath);
                textureAtlasX = coords[0];
                textureAtlasY = coords[1];
                textureAtlasW = coords[2];
                textureAtlasH = coords[3];

                this.atlasCoords = "x=" + coords[0] + "|y=" + coords[1] + "|w=" + coords[2] + "|h=" + coords[3];

                defaultTexture = screen.getAtlasTexture();

                imgWidth = defaultTexture.getImage().getWidth();
                imgHeight = defaultTexture.getImage().getHeight();
                pixelWidth = 1f / imgWidth;
                pixelHeight = 1f / imgHeight;

                textureAtlasY = imgHeight - textureAtlasY - textureAtlasH;

            } else {

                if (useAtlas) useLocalTexture = true;

                defaultTexture = assetManager.loadTexture(texturePath);
                defaultTexture.setMinFilter(Texture.MinFilter.NearestLinearMipMap);
                defaultTexture.setMagFilter(Texture.MagFilter.Bilinear);
                defaultTexture.setWrap(Texture.WrapMode.EdgeClamp);

                final Image image = defaultTexture.getImage();

                imgWidth = image.getWidth();
                imgHeight = image.getHeight();
                pixelWidth = 1f / imgWidth;
                pixelHeight = 1f / imgHeight;

                textureAtlasW = imgWidth;
                textureAtlasH = imgHeight;
            }
        }

        material = new Material(assetManager, "tonegod/gui/shaders/Unshaded.j3md");

        if (texturePath != null) {
            material.setTexture(PARAM_COLOR_MAP, defaultTexture);
            material.setColor(PARAM_COLOR, ColorRGBA.White);
        } else {
            material.setColor(PARAM_COLOR, defaultColor);
        }

        if (useAtlas) material.setBoolean(PARAM_USE_EFFECT_TEX_COORDS, true);

        material.setVector2(PARAM_OFFSET_ALPHA_TEX_COORD, new Vector2f(0, 0));
        material.setFloat(PARAM_GLOBAL_ALPHA, screen.getGlobalAlpha());

        final RenderState additionalRenderState = material.getAdditionalRenderState();
        additionalRenderState.setBlendMode(BlendMode.Alpha);
        additionalRenderState.setFaceCullMode(RenderState.FaceCullMode.Back);

        model = new ElementQuadGrid(this.dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, textureAtlasX, textureAtlasY, textureAtlasW, textureAtlasH);
        geom = new Geometry(UID + ":Geometry");
        geom.setMesh(model);
        geom.setCullHint(CullHint.Never);
        geom.setQueueBucket(Bucket.Gui);
        geom.setMaterial(material);

        setName(UID + ":Node");
        attachChild(geom);
        setQueueBucket(Bucket.Gui);
        setLocalTranslation(position.x, position.y, 0);
    }

    public Vector2f getOrgPosition() {
        return orgPosition;
    }

    public void setOrgPosition(final Vector2f orgPosition) {
        this.orgPosition.set(orgPosition);
    }

    @Override
    public void free() {
        this.initialized = false;
    }

    /**
     * Converts the the inputed percentage (0.0f-1.0f) into pixels of the elements image
     *
     * @param in Vector2f containing the x and y percentage
     * @return Vector2f containing the actual width/height in pixels
     */
    public final Vector2f getV2fPercentToPixels(final Vector2f in) {

        final Element elementParent = getElementParent();

        if (elementParent == null) {
            if (in.x < 1) in.setX(screen.getWidth() * in.x);
            if (in.y < 1) in.setY(screen.getHeight() * in.y);
        } else {
            if (in.x < 1) in.setX(elementParent.getWidth() * in.x);
            if (in.y < 1) in.setY(elementParent.getHeight() * in.y);
        }

        return in;
    }

    //<editor-fold desc="Parent/Child">

    /**
     * Adds the specified Element as a child to this Element.
     *
     * @param child The Element to add as a child
     */
    public void addChild(Element child) {
        addChild(child, false);
    }

    /**
     * Adds the specified Element as a child to this Element.
     *
     * @param child The Element to add as a child
     */
    public void addChild(final Element child, final boolean hide) {
        child.elementParent = this;

        for (final ClippingDefine clippingDefine : clippingLayers) {
            if (clippingDefine.getClipping() == null)
                child.addClippingLayer(clippingDefine.getElement());
        }

        if (!child.getInitialized()) {

            child.setY(getHeight() - child.getHeight() - child.getY());

            final Vector2f orgPosition = child.getOrgPosition();
            orgPosition.set(position);
            orgPosition.setY(child.getY());

            child.setInitialized();
        }

        child.orgRelDimensions.set(child.getWidth() / getWidth(), child.getHeight() / getHeight());
        child.setQueueBucket(RenderQueue.Bucket.Gui);

        if (screen.getElementById(child.getUID()) != null) {
            try {
                throw new ConflictingIDException();
            } catch (ConflictingIDException ex) {
                Logger.getLogger(Element.class.getName()).log(Level.SEVERE, "The child element '" + child.getUID() + "' (" + child.getClass() + ") conflicts with a previously added child element in parent element '" + getUID() + "'.", ex);
                System.exit(0);
            }
        } else {
            elementChildren.put(child.getUID(), child);
            attachChild(child);
            if (hide) child.hide();
        }

        resetChildZOrder();
    }

    /**
     * Removes the specified Element
     *
     * @param child Element to remove
     */
    public void removeChild(Element child) {

        final Element element = elementChildren.remove(child.getUID());

        if (element != null) {

            element.elementParent = null;
            element.removeFromParent();
            element.removeClippingLayer(this);

            for (final ClippingDefine define : clippingLayers) {
                element.removeClippingLayer(define.getElement());
            }

            element.cleanup();

            final ElementManager screen = getScreen();

            if (screen.getUseToolTips()) {
                if (screen.getToolTipFocus() == this) {
                    screen.hideToolTip();
                } else if (screen.getToolTipFocus() != null) {
                    if (getChildElementById(screen.getToolTipFocus().getUID()) != null) {
                        screen.hideToolTip();
                    }
                }
            }
        }

        resetChildZOrder();
    }

    /**
     * Remove all child Elements from this Element
     */
    public void removeAllChildren() {

        for (final Element element : elementChildren.values()) {

            element.removeFromParent();
            element.removeClippingLayer(this);

            for (final ClippingDefine define : clippingLayers) {
                element.removeClippingLayer(define.getElement());
            }
        }

        elementChildren.clear();
    }

    /**
     * Returns the child elements as a Map
     */
    public Map<String, Element> getElementsAsMap() {
        return elementChildren;
    }

    /**
     * Returns the child elements as a Collection
     */
    public Collection<Element> getElements() {
        return elementChildren.values();
    }

    /**
     * Returns the one and only Element's screen
     */
    public ElementManager getScreen() {
        return screen;
    }

    /**
     * Returns a list of all children that are an instance of DragElement
     *
     * @return List
     */
    public List<Element> getDraggableChildren() {

        final List<Element> elements = new ArrayList<>();

        for (final Element element : elementChildren.values()) {
            if (element instanceof DragElement) {
                elements.add(element);
            }
        }

        return elements;
    }

    /**
     * Recursively searches children elements for specified element containing the specified UID
     *
     * @param UID - Unique Indentifier of element to search for
     * @return Element containing UID or null if not found
     */
    public Element getChildElementById(final String UID) {
        if (getUID().equals(UID)) return this;
        if (elementChildren.containsKey(UID)) return elementChildren.get(UID);

        Element element = null;

        for (final Element child : elementChildren.values()) {
            element = child.getChildElementById(UID);
            if (element != null) break;
        }

        return element;
    }

    /**
     * Returns the top-most parent in the tree of Elements.  The topmost element will always have a
     * parent of null
     *
     * @return Element elementParent
     */
    public Element getAbsoluteParent() {
        if (elementParent == null) return this;
        else return elementParent.getAbsoluteParent();
    }

    /**
     * Returns the parent element of this node
     *
     * @return Element elementParent
     */
    public Element getElementParent() {
        return elementParent;
    }

    /**
     * Sets the element's parent element
     *
     * @param elementParent Element
     */
    public void setElementParent(Element elementParent) {
        this.elementParent = elementParent;
    }
    //</editor-fold>

    /**
     * Allows for setting the Element UID if (and ONLY if) the Element Parent is null
     *
     * @param UID The new UID
     * @return boolean If setting the UID was successful
     */
    public boolean setUID(final String UID) {
        if (this.elementParent == null) {
            this.UID = UID;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the element's unique string identifier
     *
     * @return String UID
     */
    public String getUID() {
        return UID;
    }

    //<editor-fold desc="Z-Order">
    private int getParentCount() {

        int count = 0;

        Element parent = getElementParent();

        while (parent != null) {
            count++;
            parent = parent.getElementParent();
        }

        return count;
    }

    public void resetChildZOrder() {

        float stepSize = screen.getZOrderStepMinor();
        float nextZOrder = stepSize;

        for (final Element element : elementChildren.values()) {

            final Vector3f elementLoc = element.getLocalTranslation();
            elementLoc.setZ(nextZOrder);

            element.setLocalTranslation(elementLoc);

            nextZOrder += stepSize;

            final BitmapText textElement = element.getTextElement();

            if (textElement != null) {

                final Vector3f textLoc = textElement.getLocalTranslation();
                textLoc.setZ(nextZOrder);

                textElement.setLocalTranslation(textLoc);

                nextZOrder += stepSize;
            }
        }
    }

    public void bringToFront() {
        if (getElementParent() == null) return;
        final Element parent = getElementParent();
        parent.removeChild(this);
        parent.addChild(this);
        parent.resetChildZOrder();
    }

    /**
     * Recursive call made by the screen control to properly initialize z-order (depth) placement
     *
     * @param zOrder The depth to place the Element at. (Relative to the parent's z-order)
     */
    protected void initZOrder(final float zOrder) {

        final Vector3f currentLoc = getLocalTranslation();
        currentLoc.setZ(zOrder);

        setLocalTranslation(currentLoc);

        final float stepSize = screen.getZOrderStepMinor();
        float nextZOrder = 0F;

        final BitmapText textElement = getTextElement();

        if (textElement != null) {

            final Vector3f textLoc = textElement.getLocalTranslation();
            textLoc.setZ(nextZOrder);

            textElement.setLocalTranslation(textLoc);

            nextZOrder += stepSize;
        }

        for (final Element children : elementChildren.values()) {
            children.initZOrder(nextZOrder);
            nextZOrder += stepSize;
        }
    }

    /**
     * Returns the Element's zOrder
     *
     * @return float zOrder
     */
    public float getZOrder() {
        return zOrder;
    }

    /**
     * Sets the Elements zOrder (I would suggest NOT using this method)
     */
    public void setZOrder(float zOrder) {
        this.zOrder = zOrder;
        initZOrder(zOrder);
    }

    public void setEffectZOrder(boolean effectZOrder) {
        this.effectZOrder = effectZOrder;
    }

    public boolean getEffectZOrder() {
        return effectZOrder;
    }
    //</editor-fold>

    //<editor-fold desc="Scaling, Docking & Other Behaviors">

    /**
     * The setAsContainer only method removes the Mesh component (rendered Mesh) from the Element,
     * leaving only Element functionality.  Call this method to set the Element for use as a parent
     * container.
     */
    public void setAsContainerOnly() {
        detachChildAt(0);
    }

    /**
     * Informs the screen control that this Element should be ignored by mouse events.
     *
     * @param ignoreMouse boolean
     */
    public void setIgnoreMouse(boolean ignoreMouse) {
        this.ignoreMouse = ignoreMouse;
        setIgnoreMouseButtons(ignoreMouse);
        setIgnoreMouseWheel(ignoreMouse);
        setIgnoreMouseFocus(ignoreMouse);
        setIgnoreTouchEvents(ignoreMouse);
    }

    /**
     * Returns if the element is set to ingnore mouse events
     *
     * @return boolean ignoreMouse
     */
    public boolean getIgnoreMouse() {
        return this.ignoreMouse;
    }

    /**
     * Element will ignore mouse left right button events
     */
    public void setIgnoreMouseButtons(boolean ignoreMouseButtons) {
        setIgnoreMouseLeftButton(ignoreMouseButtons);
        setIgnoreMouseRightButton(ignoreMouseButtons);
    }

    /**
     * Returns true if both left and right mouse buttons are being ignored
     */
    public boolean getIgnoreMouseButtons() {
        return (getIgnoreMouseLeftButton() && getIgnoreMouseRightButton());
    }

    /**
     * Element will ignore mouse left button events
     */
    public void setIgnoreMouseLeftButton(boolean ignoreMouseLeftButton) {
        this.ignoreMouseLeftButton = ignoreMouseLeftButton;
    }

    /**
     * Returns if the left mouse button is being ignored
     */
    public boolean getIgnoreMouseLeftButton() {
        return this.ignoreMouseLeftButton;
    }

    /**
     * Element will ignore mouse right button events
     */
    public void setIgnoreMouseRightButton(boolean ignoreMouseRightButton) {
        this.ignoreMouseRightButton = ignoreMouseRightButton;
    }

    /**
     * Returns if the right mouse button is being ignored
     */
    public boolean getIgnoreMouseRightButton() {
        return this.ignoreMouseRightButton;
    }

    /**
     * Element will ignore mouse focus
     */
    public void setIgnoreMouseFocus(boolean ignoreMouseFocus) {
        this.ignoreMouseFocus = ignoreMouseFocus;
    }

    /**
     * Returns if the element ignores mouse focus
     */
    public boolean getIgnoreMouseFocus() {
        return this.ignoreMouseFocus;
    }

    /**
     * Element will ignore mouse wheel click and move events
     */
    public void setIgnoreMouseWheel(boolean ignoreMouseWheel) {
        setIgnoreMouseWheelClick(ignoreMouseWheel);
        setIgnoreMouseWheelMove(ignoreMouseWheel);
    }

    /**
     * Returns if the element is ignoring both mouse wheel click and move events
     */
    public boolean getIgnoreMouseWheel() {
        return (getIgnoreMouseWheelClick() && getIgnoreMouseWheelMove());
    }

    /**
     * Element will ignore mouse wheel click events
     */
    public void setIgnoreMouseWheelClick(boolean ignoreMouseWheelClick) {
        this.ignoreMouseWheelClick = ignoreMouseWheelClick;
    }

    /**
     * Returns if the element ignores mouse wheel clicks
     */
    public boolean getIgnoreMouseWheelClick() {
        return this.ignoreMouseWheelClick;
    }

    /**
     * Element will ignore mouse wheel mouse events;
     */
    public void setIgnoreMouseWheelMove(boolean ignoreMouseWheelMove) {
        this.ignoreMouseWheelMove = ignoreMouseWheelMove;
    }

    /**
     * Returns if the element is ignoring mouse wheel moves
     */
    public boolean getIgnoreMouseWheelMove() {
        return this.ignoreMouseWheelMove;
    }

    /**
     * Element will ignore touch down up move and fling events
     */
    public void setIgnoreTouchEvents(boolean ignoreTouchEvents) {
        setIgnoreTouch(ignoreTouchEvents);
        setIgnoreTouchMove(ignoreTouchEvents);
        setIgnoreFling(ignoreTouchEvents);
    }

    /**
     * Returns if the element ignores touch down, up, move fling events
     */
    public boolean getIgnoreTouchEvents() {
        return (getIgnoreTouch() && getIgnoreTouchMove() && getIgnoreFling());
    }

    /**
     * Element will ignore touch down and up events
     */
    public void setIgnoreTouch(boolean ignoreTouch) {
        this.ignoreTouch = ignoreTouch;
    }

    /**
     * Returns if the element ignores touch down and up events
     */
    public boolean getIgnoreTouch() {
        return ignoreTouch;
    }

    /**
     * element will ignore touch move events
     */
    public void setIgnoreTouchMove(boolean ignoreTouchMove) {
        this.ignoreTouchMove = ignoreTouchMove;
    }

    /**
     * Returns if the element ignores touch move events
     */
    public boolean getIgnoreTouchMove() {
        return this.ignoreTouchMove;
    }

    /**
     * Element will ignore touch fling events
     */
    public void setIgnoreFling(boolean ignoreFling) {
        this.ignoreFling = ignoreFling;
    }

    /**
     * Returns if the element ignores fling events
     */
    public boolean getIgnoreFling() {
        return this.ignoreFling;
    }

    /**
     * Enables draggable behavior for this element
     *
     * @param isMovable boolean
     */
    public void setIsMovable(boolean isMovable) {
        this.isMovable = isMovable;
    }

    /**
     * Returns if the element has draggable behavior set
     *
     * @return boolean isMovable
     */
    public boolean getIsMovable() {
        return this.isMovable;
    }

    /**
     * Enables resize behavior for this element
     *
     * @param isResizable boolean
     */
    public void setIsResizable(boolean isResizable) {
        this.isResizable = isResizable;
    }

    /**
     * Returns if the element has resize behavior set
     *
     * @return boolean isResizable
     */
    public boolean getIsResizable() {
        return this.isResizable;
    }

    /**
     * Enables/disables north border for resizing
     *
     * @param resizeN boolean
     */
    public void setResizeN(boolean resizeN) {
        this.resizeS = resizeN;
    }

    /**
     * Returns whether the elements north border has enabled/disabled resizing
     *
     * @return boolean resizeN
     */
    public boolean getResizeN() {
        return this.resizeS;
    }

    /**
     * Enables/disables south border for resizing
     *
     * @param resizeS boolean
     */
    public void setResizeS(boolean resizeS) {
        this.resizeN = resizeS;
    }

    /**
     * Returns whether the elements south border has enabled/disabled resizing
     *
     * @return boolean resizeS
     */
    public boolean getResizeS() {
        return this.resizeN;
    }

    /**
     * Enables/disables west border for resizing
     *
     * @param resizeW boolean
     */
    public void setResizeW(boolean resizeW) {
        this.resizeW = resizeW;
    }

    /**
     * Returns whether the elements west border has enabled/disabled resizing
     *
     * @return boolean resizeW
     */
    public boolean getResizeW() {
        return this.resizeW;
    }

    /**
     * Enables/disables east border for resizing
     *
     * @param resizeE boolean
     */
    public void setResizeE(boolean resizeE) {
        this.resizeE = resizeE;
    }

    /**
     * Returns whether the elements east border has enabled/disabled resizing
     *
     * @return boolean resizeE
     */
    public boolean getResizeE() {
        return this.resizeE;
    }

    /**
     * Sets how the element will docking to it's parent element during resize events. NW = Top Left
     * of parent element NE = Top Right of parent element SW = Bottom Left of parent element SE =
     * Bottom Right of parent element
     */
    public void setDocking(Docking docking) {
        this.docking = docking;
    }

    public Docking getDocking() {
        return docking;
    }

    /**
     * Enables north docking of element (disables south docking of Element).  This determines how
     * the Element should retain positioning on parent resize events.
     *
     * @param dockN boolean
     */
    @Deprecated
    public void setDockN(boolean dockN) {
        this.dockS = dockN;
        this.dockN = !dockN;

        Docking docking = null;
        if (dockS) {
            if (dockE) docking = Docking.NE;
            else docking = Docking.NW;
        } else {
            if (dockE) docking = Docking.SE;
            else docking = Docking.SW;
        }

        setDocking(docking);
    }

    /**
     * Returns if the Element is docked to the north quadrant of it's parent element.
     *
     * @return boolean dockN
     */
    @Deprecated
    public boolean getDockN() {
        return dockS;
    }

    /**
     * Enables west docking of Element (disables east docking of Element).  This determines how the
     * Element should retain positioning on parent resize events.
     *
     * @param dockW boolean
     */
    @Deprecated
    public void setDockW(boolean dockW) {
        this.dockW = dockW;
        this.dockE = !dockW;

        Docking docking = null;

        if (dockE) {
            if (dockS) docking = Docking.NE;
            else docking = Docking.SE;
        } else {
            if (dockS) docking = Docking.NW;
            else docking = Docking.SW;
        }

        setDocking(docking);
    }

    /**
     * Returns if the Element is docked to the west quadrant of it's parent element.
     *
     * @return boolean dockW
     */
    @Deprecated
    public boolean getDockW() {
        return dockW;
    }

    /**
     * Enables east docking of Element (disables west docking of Element).  This determines how the
     * Element should retain positioning on parent resize events.
     *
     * @param dockE boolean
     */
    @Deprecated
    public void setDockE(boolean dockE) {
        this.dockE = dockE;
        this.dockW = !dockE;

        Docking docking = null;
        if (dockE) {
            if (dockS) docking = Docking.NE;
            else docking = Docking.SE;
        } else {
            if (dockS) docking = Docking.NW;
            else docking = Docking.SW;
        }

        setDocking(docking);
    }

    /**
     * Returns if the Element is docked to the east quadrant of it's parent element.
     *
     * @return boolean dockE
     */
    @Deprecated
    public boolean getDockE() {
        return this.dockE;
    }

    /**
     * Enables south docking of Element (disables north docking of Element).  This determines how
     * the Element should retain positioning on parent resize events.
     *
     * @param dockS boolean
     */
    @Deprecated
    public void setDockS(boolean dockS) {
        this.dockN = dockS;
        this.dockS = !dockS;

        Docking docking = null;

        if (dockS) {
            if (dockE) docking = Docking.NE;
            else docking = Docking.NW;
        } else {
            if (dockE) docking = Docking.SE;
            else docking = Docking.SW;
        }

        setDocking(docking);
    }

    /**
     * Returns if the Element is docked to the south quadrant of it's parent element.
     *
     * @return boolean dockS
     */
    @Deprecated
    public boolean getDockS() {
        return this.dockN;
    }

    /**
     * Determines if the element should scale with parent when resized vertically.
     *
     * @param scaleNS boolean
     */
    public void setScaleNS(boolean scaleNS) {
        this.scaleNS = scaleNS;
    }

    /**
     * Returns if the Element is set to scale vertically when it's parent Element is resized.
     *
     * @return boolean scaleNS
     */
    public boolean getScaleNS() {
        return this.scaleNS;
    }

    /**
     * Determines if the element should scale with parent when resized horizontally.
     *
     * @param scaleEW boolean
     */
    public void setScaleEW(boolean scaleEW) {
        this.scaleEW = scaleEW;
    }

    /**
     * Returns if the Element is set to scale horizontally when it's parent Element is resized.
     *
     * @return boolean scaleEW
     */
    public boolean getScaleEW() {
        return this.scaleEW;
    }

    /**
     * Sets the element to pass certain events (movement, resizing) to it direct parent instead of
     * effecting itself.
     *
     * @param effectParent boolean
     */
    public void setEffectParent(boolean effectParent) {
        this.effectParent = effectParent;
    }

    /**
     * Returns if the Element is set to pass events to it's direct parent
     *
     * @return boolean effectParent
     */
    public boolean getEffectParent() {
        return this.effectParent;
    }

    /**
     * Sets the element to pass certain events (movement, resizing) to it absolute parent instead of
     * effecting itself.
     *
     * The Elements absolute parent is the element farthest up in it's nesting order, or simply put,
     * was added to the screen.
     *
     * @param effectAbsoluteParent boolean
     */
    public void setEffectAbsoluteParent(boolean effectAbsoluteParent) {
        this.effectAbsoluteParent = effectAbsoluteParent;
    }

    /**
     * Returns if the Element is set to pass events to it's absolute parent
     *
     * @return boolean effectParent
     */
    public boolean getEffectAbsoluteParent() {
        return this.effectAbsoluteParent;
    }

    /**
     * Forces the object to stay within the constraints of it's parent Elements dimensions. NOTE:
     * use setLockToParentBounds instead.
     *
     * @param lockToParentBounds boolean
     */
    @Deprecated
    public void setlockToParentBounds(boolean lockToParentBounds) {
        this.lockToParentBounds = lockToParentBounds;
    }

    /**
     * Forces the object to stay within the constraints of it's parent Elements dimensions.
     *
     * @param lockToParentBounds boolean
     */
    public void setLockToParentBounds(boolean lockToParentBounds) {
        this.lockToParentBounds = lockToParentBounds;
    }

    /**
     * Returns if the Element has been constrained to it's parent Element's dimensions.
     *
     * @return boolean lockToParentBounds
     */
    public boolean getLockToParentBounds() {
        return this.lockToParentBounds;
    }

    public void setGlobalUIScale(float widthPercent, float heightPercent) {
        for (final Element element : elementChildren.values()) {
            element.setPosition(element.getPosition().x * widthPercent, element.getPosition().y * heightPercent);
            element.setDimensions(element.getDimensions().x * widthPercent, element.getDimensions().y * heightPercent);
            element.setFontSize(element.getFontSize() * heightPercent);
            element.setGlobalUIScale(widthPercent, heightPercent);
        }
    }

    /**
     * Allows for dynamically enabling/disabling the element
     *
     * @param isEnabled boolean
     */
    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        controlIsEnabledHook(isEnabled);
        for (final Element element : elementChildren.values()) {
            element.setIsEnabled(isEnabled);
        }
    }

    public void controlIsEnabledHook(boolean isEnabled) {
    }

    /**
     * Returns if the element is currently enabled
     *
     * @return boolean
     */
    public boolean getIsEnabled() {
        return this.isEnabled;
    }
    //</editor-fold>

    //<editor-fold desc="Drag & Drop Support">

    /**
     * Flags Element as Drag Element for Drag Drop interaction
     *
     * @param isDragElement boolean
     */
    public void setIsDragDropDragElement(boolean isDragElement) {
        this.isDragElement = isDragElement;
        if (isDragElement)
            this.isDropElement = false;
    }

    /**
     * Returns if the Element is currently flagged as a Drag Element for Drag Drop interaction
     *
     * @return boolean
     */
    public boolean getIsDragDropDragElement() {
        return this.isDragElement;
    }

    /**
     * Flags Element as Drop Element for Drag Drop interaction
     *
     * @param isDropElement boolean
     */
    public void setIsDragDropDropElement(boolean isDropElement) {
        this.isDropElement = isDropElement;
        if (isDropElement) isDragElement = false;
    }

    /**
     * Returns if the Element is currently flagged as a Drop Element for Drag Drop interaction
     *
     * @return boolean
     */
    public boolean getIsDragDropDropElement() {
        return isDropElement;
    }
    //</editor-fold>

    //<editor-fold desc="Sizing & Positioning">

    /**
     * Set the x,y coordinates of the Element.  X and y are relative to the parent Element.
     *
     * @param position Vector2f screen poisition of Element
     */
    public void setPosition(Vector2f position) {
        this.position.set(position);
        updateNodeLocation();
    }

    /**
     * Set the x,y coordinates of the Element.  X and y are relative to the parent Element.
     *
     * @param x The x coordinate screen poisition of Element
     * @param y The y coordinate screen poisition of Element
     */
    public void setPosition(float x, float y) {
        this.position.setX(x);
        this.position.setY(y);
        updateNodeLocation();
    }

    /**
     * Set the x coordinates of the Element.  X is relative to the parent Element.
     *
     * @param x The x coordinate screen poisition of Element
     */
    public void setX(float x) {
        this.position.setX(x);
        updateNodeLocation();
    }

    /**
     * Set the y coordinates of the Element.  Y is relative to the parent Element.
     *
     * @param y The y coordinate screen poisition of Element
     */
    public void setY(float y) {
        this.position.setY(y);
        updateNodeLocation();
    }

    private void updateNodeLocation() {
        this.setLocalTranslation(position.x, position.y, this.getLocalTranslation().getZ());
        updateClippingLayers();
    }

    /**
     * Returns the current screen location of the Element
     *
     * @return Vector2f position
     */
    public Vector2f getPosition() {
        return position;
    }

    /**
     * Gets the relative x coordinate of the Element from it's parent Element's x
     *
     * @return float
     */
    public float getX() {
        return position.x;
    }

    /**
     * Gets the relative y coordinate of the Element from it's parent Element's y
     *
     * @return float
     */
    public float getY() {
        return position.y;
    }

    /**
     * Returns the x coord of an element from screen x 0, ignoring the nesting order.
     *
     * @return float x
     */
    public float getAbsoluteX() {

        float x = getX();
        Element element = this;

        while (element.getElementParent() != null) {
            element = element.getElementParent();
            x += element.getX();
        }

        return x;
    }

    /**
     * Returns the y coord of an element from screen y 0, ignoring the nesting order.
     *
     * @return float
     */
    public float getAbsoluteY() {

        float y = getY();
        Element element = this;

        while (element.getElementParent() != null) {
            element = element.getElementParent();
            y += element.getY();
        }

        return y;
    }

    /**
     * Sets the width and height of the element
     *
     * @param width  float
     * @param height float
     */
    public void setDimensions(final float width, final float height) {
        dimensions.setX(width);
        dimensions.setY(height);

        final ElementQuadGrid model = getModel();
        model.updateDimensions(dimensions.x, dimensions.y);

        if (tileImage) {
            float tcW = dimensions.x / model.getImageWidth();
            float tcH = dimensions.y / model.getImageHeight();
            model.updateTiledTexCoords(0, -tcH, tcW, 0);
        }

        geom.updateModelBound();

        if (textElement != null) updateTextElement();

        updateClippingLayers();
    }

    /**
     * Sets the width and height of the element
     *
     * @param dimensions Vector2f
     */
    public void setDimensions(Vector2f dimensions) {
        this.dimensions.set(dimensions);

        final ElementQuadGrid model = getModel();
        model.updateDimensions(dimensions.x, dimensions.y);

        if (tileImage) {
            float tcW = dimensions.x / model.getImageWidth();
            float tcH = dimensions.y / model.getImageHeight();
            model.updateTiledTexCoords(0, -tcH, tcW, 0);
        }

        geom.updateModelBound();

        if (textElement != null) updateTextElement();

        updateClippingLayers();
    }

    /**
     * Stubbed for future use.  This should limit resizing to the minimum dimensions defined
     *
     * @param minDimensions The absolute minimum dimensions for this Element.
     */
    public void setMinDimensions(Vector2f minDimensions) {
        if (this.minDimensions == null) this.minDimensions = new Vector2f();
        this.minDimensions.set(minDimensions);
    }

    public Vector2f getMinDimensions() {
        return this.minDimensions;
    }

    /**
     * Sets the width of the element
     *
     * @param width float
     */
    public void setWidth(final float width) {
        dimensions.setX(width);

        final ElementQuadGrid model = getModel();
        model.updateWidth(dimensions.x);

        if (tileImage) {
            float tcW = dimensions.x / model.getImageWidth();
            float tcH = dimensions.y / model.getImageHeight();
            model.updateTiledTexCoords(0, -tcH, tcW, 0);
        }

        geom.updateModelBound();

        if (textElement != null) updateTextElement();

        updateClippingLayers();
    }

    /**
     * Sets the height of the element
     *
     * @param height float
     */
    public void setHeight(final float height) {
        dimensions.setY(height);

        final ElementQuadGrid model = getModel();
        model.updateHeight(dimensions.y);

        if (tileImage) {
            float tcW = dimensions.x / model.getImageWidth();
            float tcH = dimensions.y / model.getImageHeight();
            model.updateTiledTexCoords(0, -tcH, tcW, 0);
        }

        geom.updateModelBound();

        if (textElement != null) updateTextElement();

        updateClippingLayers();
    }

    /**
     * Returns a Vector2f containing the actual width and height of an Element
     *
     * @return float
     */
    public Vector2f getDimensions() {
        return dimensions;
    }

    /**
     * Returns the dimensions defined at the time of the Element's creation.
     */
    public Vector2f getOrgDimensions() {
        return orgDimensions;
    }

    /**
     * Returns the actual width of an Element
     *
     * @return float
     */
    public float getWidth() {
        return dimensions.x;
    }

    /**
     * Returns the actual height of an Element
     *
     * @return float
     */
    public float getHeight() {
        return dimensions.y;
    }

    /**
     * Returns the width of an Element from screen x 0
     *
     * @return float
     */
    public float getAbsoluteWidth() {
        return getAbsoluteX() + getWidth();
    }

    /**
     * Returns the height of an Element from screen y 0
     *
     * @return float
     */
    public float getAbsoluteHeight() {
        return getAbsoluteY() + getHeight();
    }
    //</editor-fold>

    /**
     * Stubbed for future use.
     */
    public void validateLayout() {

        final Vector2f dimensions = getDimensions();

        if (dimensions.x < 1 || dimensions.y < 1) {
            final Vector2f dim = getV2fPercentToPixels(dimensions);
            resize(getAbsoluteX() + dim.x, getAbsoluteY() + dim.y, Element.Borders.SE);
        }

        final Vector2f position = getPosition();

        if (position.x < 1 || position.y < 1) {
            Vector2f pos = getV2fPercentToPixels(position);
            setPosition(pos.x, pos.y);
        }

        if (getElementParent() != null) {
            setY(getElementParent().getHeight() - getHeight() - getY());
        } else {
            setY(screen.getHeight() - getHeight() - getY());
        }

        for (final Element element : elementChildren.values()) {
            element.validateLayout();
        }
    }

    /**
     * Stubbed for future use.
     */
    public void setInitialized() {
        this.initialized = true;
    }

    /**
     * Stubbed for future use.
     */
    public boolean getInitialized() {
        return initialized;
    }

    //<editor-fold desc="Resize & Move">

    /**
     * The preferred method for resizing Elements if the resize must effect nested Elements as
     * well.
     *
     * @param x   the absolute x coordinate from screen x 0
     * @param y   the absolute y coordinate from screen y 0
     * @param dir The Element.Borders used to determine the direction of the resize event
     */
    public void resize(float x, float y, Borders dir) {
        float prevWidth = getWidth();
        float prevHeight = getHeight();
        float oX = x, oY = y;
        if (getElementParent() != null) {
            x -= getAbsoluteX() - getX();
        }
        if (getElementParent() != null) {
            y -= getAbsoluteY() - getY();
        }
        float nextX, nextY;
        if (dir == Borders.NW) {
            if (getLockToParentBounds()) {
                if (x <= 0) {
                    x = 0;
                }
            }
            if (minDimensions != null) {
                if (getX() + getWidth() - x <= minDimensions.x) {
                    x = getX() + getWidth() - minDimensions.x;
                }
            }
            if (resizeW) {
                setWidth(getX() + getWidth() - x);
                setX(x);
            }
            if (getLockToParentBounds()) {
                if (y <= 0) {
                    y = 0;
                }
            }
            if (minDimensions != null) {
                if (getY() + getHeight() - y <= minDimensions.y) {
                    y = getY() + getHeight() - minDimensions.y;
                }
            }
            if (resizeN) {
                setHeight(getY() + getHeight() - y);
                setY(y);
            }
        } else if (dir == Borders.N) {
            if (getLockToParentBounds()) {
                if (y <= 0) {
                    y = 0;
                }
            }
            if (minDimensions != null) {
                if (getY() + getHeight() - y <= minDimensions.y) {
                    y = getY() + getHeight() - minDimensions.y;
                }
            }
            if (resizeN) {
                setHeight(getY() + getHeight() - y);
                setY(y);
            }
        } else if (dir == Borders.NE) {
            nextX = oX - getAbsoluteX();
            if (getLockToParentBounds()) {
                float checkWidth = (getElementParent() == null) ? screen.getWidth() : getElementParent().getWidth();
                if (nextX >= checkWidth - getX()) {
                    nextX = checkWidth - getX();
                }
            }
            if (minDimensions != null) {
                if (nextX <= minDimensions.x) {
                    nextX = minDimensions.x;
                }
            }
            if (resizeE) {
                setWidth(nextX);
            }
            if (getLockToParentBounds()) {
                if (y <= 0) {
                    y = 0;
                }
            }
            if (minDimensions != null) {
                if (getY() + getHeight() - y <= minDimensions.y) {
                    y = getY() + getHeight() - minDimensions.y;
                }
            }
            if (resizeN) {
                setHeight(getY() + getHeight() - y);
                setY(y);
            }
        } else if (dir == Borders.W) {
            if (getLockToParentBounds()) {
                if (x <= 0) {
                    x = 0;
                }
            }
            if (minDimensions != null) {
                if (getX() + getWidth() - x <= minDimensions.x) {
                    x = getX() + getWidth() - minDimensions.x;
                }
            }
            if (resizeW) {
                setWidth(getX() + getWidth() - x);
                setX(x);
            }
        } else if (dir == Borders.E) {
            nextX = oX - getAbsoluteX();
            if (getLockToParentBounds()) {
                float checkWidth = (getElementParent() == null) ? screen.getWidth() : getElementParent().getWidth();
                if (nextX >= checkWidth - getX()) {
                    nextX = checkWidth - getX();
                }
            }
            if (minDimensions != null) {
                if (nextX <= minDimensions.x) {
                    nextX = minDimensions.x;
                }
            }
            if (resizeE) {
                setWidth(nextX);
            }
        } else if (dir == Borders.SW) {
            if (getLockToParentBounds()) {
                if (x <= 0) {
                    x = 0;
                }
            }
            if (minDimensions != null) {
                if (getX() + getWidth() - x <= minDimensions.x) {
                    x = getX() + getWidth() - minDimensions.x;
                }
            }
            if (resizeW) {
                setWidth(getX() + getWidth() - x);
                setX(x);
            }
            nextY = oY - getAbsoluteY();
            if (getLockToParentBounds()) {
                float checkHeight = (getElementParent() == null) ? screen.getHeight() : getElementParent().getHeight();
                if (nextY >= checkHeight - getY()) {
                    nextY = checkHeight - getY();
                }
            }
            if (minDimensions != null) {
                if (nextY <= minDimensions.y) {
                    nextY = minDimensions.y;
                }
            }
            if (resizeS) {
                setHeight(nextY);
            }
        } else if (dir == Borders.S) {
            nextY = oY - getAbsoluteY();
            if (getLockToParentBounds()) {
                float checkHeight = (getElementParent() == null) ? screen.getHeight() : getElementParent().getHeight();
                if (nextY >= checkHeight - getY()) {
                    nextY = checkHeight - getY();
                }
            }
            if (minDimensions != null) {
                if (nextY <= minDimensions.y) {
                    nextY = minDimensions.y;
                }
            }
            if (resizeS) {
                setHeight(nextY);
            }
        } else if (dir == Borders.SE) {
            nextX = oX - getAbsoluteX();
            if (getLockToParentBounds()) {
                float checkWidth = (getElementParent() == null) ? screen.getWidth() : getElementParent().getWidth();
                if (nextX >= checkWidth - getX()) {
                    nextX = checkWidth - getX();
                }
            }
            if (minDimensions != null) {
                if (nextX <= minDimensions.x) {
                    nextX = minDimensions.x;
                }
            }
            if (resizeE) {
                setWidth(nextX);
            }
            nextY = oY - getAbsoluteY();
            if (getLockToParentBounds()) {
                float checkHeight = (getElementParent() == null) ? screen.getHeight() : getElementParent().getHeight();
                if (nextY >= checkHeight - getY()) {
                    nextY = checkHeight - getY();
                }
            }
            if (minDimensions != null) {
                if (nextY <= minDimensions.y) {
                    nextY = minDimensions.y;
                }
            }
            if (resizeS) {
                setHeight(nextY);
            }
        }
        float diffX = prevWidth - getWidth();
        float diffY = prevHeight - getHeight();
        controlResizeHook();
        boolean childResize = true;
        if (layout != null) {
            if (layout.getHandlesResize()) {
                childResize = false;
                layout.resize();
            }
        }
        if (childResize) {
            for (Element el : elementChildren.values()) {
                el.childResize(diffX, diffY, dir);
                el.controlResizeHook();
            }
        }
    }

    // TODO: enforce minimum size
    private void childResize(float diffX, float diffY, Borders dir) {
        boolean minSize = !(minDimensions == null);
        if (dir == Borders.NW || dir == Borders.N || dir == Borders.NE) {
            if (getScaleNS()) setHeight(getHeight() - diffY);
            if ((getDocking() == Docking.NW || getDocking() == Docking.NE) && !getScaleNS())
                setY(getY() - diffY);
        } else if (dir == Borders.SW || dir == Borders.S || dir == Borders.SE) {
            if (getScaleNS()) setHeight(getHeight() - diffY);
            if ((getDocking() == Docking.NW || getDocking() == Docking.NE) && !getScaleNS())
                setY(getY() - diffY);
        }
        if (dir == Borders.NW || dir == Borders.W || dir == Borders.SW) {
            if (getScaleEW()) setWidth(getWidth() - diffX);
            if ((getDocking() == Docking.NE || getDocking() == Docking.SE) && !getScaleEW())
                setX(getX() - diffX);
        } else if (dir == Borders.NE || dir == Borders.E || dir == Borders.SE) {
            if (getScaleEW()) setWidth(getWidth() - diffX);
            if ((getDocking() == Docking.NE || getDocking() == Docking.SE) && !getScaleEW())
                setX(getX() - diffX);
        }
        for (Element el : elementChildren.values()) {
            el.childResize(diffX, diffY, dir);
            el.controlResizeHook();
        }
    }

    /**
     * Overridable method for extending the resize event
     */
    public void controlResizeHook() {

    }

    public void sizeToContent() {
        float innerX = 10000, innerY = 10000, innerW = -10000, innerH = -10000;
        float currentHeight = getHeight();
        Map<Element, Float> newY = new HashMap();
        for (Element child : elementChildren.values()) {
            float x = child.getX();
            float y = currentHeight - (child.getY() + child.getHeight());
            float w = child.getX() + child.getWidth();
            float h = currentHeight - child.getY();
            if (x < innerX) innerX = x;
            if (y < innerY) innerY = y;
            if (w > innerW) innerW = w;
            if (h > innerH) innerH = h;
            newY.put(child, h);
        }
        this.setDimensions(innerW + innerX, innerH + innerY);
        for (Element child : elementChildren.values()) {
            float diff = newY.get(child);
            child.setY(innerH - (diff - innerY));
        }
    }

    /**
     * Moves the Element to the specified coordinates
     *
     * @param x The new x screen coordinate of the Element
     * @param y The new y screen coordinate of the Element
     */
    public void moveTo(float x, float y) {
        if (getLockToParentBounds()) {
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            if (getElementParent() != null) {
                if (x > getElementParent().getWidth() - getWidth()) {
                    x = getElementParent().getWidth() - getWidth();
                }
                if (y > getElementParent().getHeight() - getHeight()) {
                    y = getElementParent().getHeight() - getHeight();
                }
            } else {
                if (x > screen.getWidth() - getWidth()) {
                    x = screen.getWidth() - getWidth();
                }
                if (y > screen.getHeight() - getHeight()) {
                    y = screen.getHeight() - getHeight();
                }
            }
        }
        setX(x);
        setY(y);
        controlMoveHook();
    }

    /**
     * Overridable method for extending the move event
     */
    public void controlMoveHook() {

    }
    //</editor-fold>

    //<editor-fold desc="Auto Centering">

    /**
     * Centers the Element to it's parent Element.  If the parent element is null, it will use the
     * screen's width/height.
     */
    public void centerToParent() {
        if (elementParent == null) {
            setPosition(screen.getWidth() / 2 - (getWidth() / 2), screen.getHeight() / 2 - (getHeight() / 2));
        } else {
            setPosition(elementParent.getWidth() / 2 - (getWidth() / 2), elementParent.getHeight() / 2 - (getHeight() / 2));
        }
    }

    public void centerToParentV() {
        if (elementParent == null) {
            setPosition(getX(), screen.getHeight() / 2 - (getHeight() / 2));
        } else {
            setPosition(getX(), elementParent.getHeight() / 2 - (getHeight() / 2));
        }
    }

    public void centerToParentH() {
        if (elementParent == null) {
            setPosition(screen.getWidth() / 2 - (getWidth() / 2), getY());
        } else {
            setPosition(elementParent.getWidth() / 2 - (getWidth() / 2), getY());
        }
    }
    //</editor-fold>

    //<editor-fold desc="Resze Borders">

    /**
     * Set the north, west, east and south borders in number of pixels
     */
    public void setResizeBorders(float borderSize) {
        borders.set(borderSize, borderSize, borderSize, borderSize);
    }

    /**
     * Set the north, west, east and south borders in number of pixels
     *
     * @param nBorder float
     * @param wBorder float
     * @param eBorder float
     * @param sBorder float
     */
    public void setResizeBorders(float nBorder, float wBorder, float eBorder, float sBorder) {
        borders.setX(nBorder);
        borders.setY(wBorder);
        borders.setZ(eBorder);
        borders.setW(sBorder);
    }

    /**
     * Sets the width of north border in number of pixels
     *
     * @param nBorder float
     */
    public void setNorthResizeBorder(float nBorder) {
        borders.setX(nBorder);
    }

    /**
     * Sets the width of west border in number of pixels
     *
     * @param wBorder float
     */
    public void setWestResizeBorder(float wBorder) {
        borders.setY(wBorder);
    }

    /**
     * Sets the width of east border in number of pixels
     *
     * @param eBorder float
     */
    public void setEastResizeBorder(float eBorder) {
        borders.setZ(eBorder);
    }

    /**
     * Sets the width of south border in number of pixels
     *
     * @param sBorder float
     */
    public void setSouthResizeBorder(float sBorder) {
        borders.setW(sBorder);
    }

    /**
     * Returns the height of the north resize border
     *
     * @return float
     */
    public float getResizeBorderNorthSize() {
        return this.borderHandles.x;
    }

    /**
     * Returns the width of the west resize border
     *
     * @return float
     */
    public float getResizeBorderWestSize() {
        return this.borderHandles.y;
    }

    /**
     * Returns the width of the east resize border
     *
     * @return float
     */
    public float getResizeBorderEastSize() {
        return this.borderHandles.z;
    }

    /**
     * Returns the height of the south resize border
     *
     * @return float
     */
    public float getResizeBorderSouthSize() {
        return this.borderHandles.w;
    }
    //</editor-fold>

    //<editor-fold desc="Mesh & Geometry">

    /**
     * Returns a pointer to the custom mesh used to render the Element.
     *
     * @return ElementGridQuad model
     */
    public ElementQuadGrid getModel() {
        return this.model;
    }

    /**
     * Returns the Element's Geometry.
     */
    public Geometry getGeometry() {
        return this.geom;
    }
    //</editor-fold>

    //<editor-fold desc="Fonts & Text">

    /**
     * Sets the element's text layer font
     *
     * @param fontPath String The font asset path
     */
    public void setFont(String fontPath) {
        font = app.getAssetManager().loadFont(fontPath);
        if (textElement != null) {
            String text = this.getText();
            textElement.removeFromParent();
            textElement = null;
            setText(text);
        }
    }

    /**
     * Returns the Bitmapfont used by the element's text layer
     *
     * @return BitmapFont font
     */
    public BitmapFont getFont() {
        return this.font;
    }

    /**
     * Sets the element's text layer font size
     *
     * @param fontSize float The size to set the font to
     */
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        if (textElement != null) {
            textElement.setSize(fontSize);
        }
    }

    /**
     * Returns the element's text layer font size
     *
     * @return float fontSize
     */
    public float getFontSize() {
        return this.fontSize;
    }

    /**
     * Sets the element's text layer font color
     *
     * @param fontColor ColorRGBA The color to set the font to
     */
    public void setFontColor(ColorRGBA fontColor) {
        this.fontColor = fontColor;
        if (textElement != null) {
            textElement.setColor(fontColor);
        }
    }

    /**
     * Return the element's text layer font color
     *
     * @return ColorRGBA fontColor
     */
    public ColorRGBA getFontColor() {
        return this.fontColor;
    }

    /**
     * Sets the element's text layer horizontal alignment
     */
    public void setTextAlign(BitmapFont.Align textAlign) {
        this.textAlign = textAlign;
        if (textElement != null) {
            textElement.setAlignment(textAlign);
        }
    }

    /**
     * Returns the element's text layer horizontal alignment
     *
     * @return Align text Align
     */
    public BitmapFont.Align getTextAlign() {
        return this.textAlign;
    }

    /**
     * Sets the element's text layer vertical alignment
     */
    public void setTextVAlign(BitmapFont.VAlign textVAlign) {
        this.textVAlign = textVAlign;
        if (textElement != null) {
            textElement.setVerticalAlignment(textVAlign);
        }
    }

    /**
     * Returns the element's text layer vertical alignment
     *
     * @return VAlign textVAlign
     */
    public BitmapFont.VAlign getTextVAlign() {
        return this.textVAlign;
    }

    /**
     * Sets the element's text later wrap mode
     *
     * @param textWrap LineWrapMode textWrap
     */
    public void setTextWrap(LineWrapMode textWrap) {
        this.textWrap = textWrap;
        if (textElement != null) {
            textElement.setLineWrapMode(textWrap);
        }
    }

    /**
     * Returns the element's text layer wrap mode
     *
     * @return LineWrapMode textWrap
     */
    public LineWrapMode getTextWrap() {
        return this.textWrap;
    }

    /**
     * Sets the elements text layer position
     *
     * @param x Position's x coord
     * @param y Position's y coord
     */
    public void setTextPosition(float x, float y) {
        this.textPosition = new Vector2f(x, y);
    }

    /**
     * Returns the current x, y coords of the element's text layer
     *
     * @return Vector2f textPosition
     */
    public Vector2f getTextPosition() {
        return this.textPosition;
    }

    /**
     * Sets the padding set for the element's text layer
     */
    public void setTextPadding(float textPadding) {
        setTextPadding(textPadding, textPadding, textPadding, textPadding);
    }

    public void setTextPadding(float left, float right, float top, float bottom) {
        this.textPadding.set(left, right, top, bottom);
    }

    public void setTextPadding(Vector4f textPadding) {
        this.textPadding.set(textPadding);
    }

    /**
     * Returns the ammount of padding set for the elements text layer
     *
     * @return float textPadding
     */
    public float getTextPadding() {
        return this.textPadding.x;
    }

    public Vector4f getTextPaddingVec() {
        return this.textPadding;
    }

    /**
     * Updates the element's textlayer position and boundary
     */
    protected void updateTextElement() {
        if (textElement != null) {
            textElement.setLocalTranslation(textPosition.x + textPadding.x, getHeight() - (textPosition.y + textPadding.z), textElement.getLocalTranslation().z);
            textElement.setBox(new Rectangle(0, 0, dimensions.x - (textPadding.x + textPadding.y), dimensions.y - (textPadding.z + textPadding.w)));
        }
    }

    public void resetTextElement() {
        if (textElement != null) {
            textElement.setLocalTranslation(textPosition.x + textPadding.x, getHeight() - (textPosition.y + textPadding.z), textElement.getLocalTranslation().z);
            textElement.setBox(new Rectangle(0, 0, dimensions.x - (textPadding.x + textPadding.y), 25));
        }
    }

    public void removeTextElement() {
        if (textElement != null) {
            textElement.removeFromParent();
            textElement = null;
        }
    }

    /**
     * Sets the text of the element.
     *
     * @param text String The text to display.
     */
    public void setText(String text) {
        this.text = text;

        if (textElement == null) {
            textElement = new BitmapText(font, false);
            textElement.setBox(new Rectangle(0, 0, dimensions.x, dimensions.y));
            //	textElement = new TextElement(screen, Vector2f.ZERO, getDimensions());
        }
        textElement.setLineWrapMode(textWrap);
        textElement.setAlignment(textAlign);
        textElement.setVerticalAlignment(textVAlign);
        textElement.setSize(fontSize);
        textElement.setColor(fontColor);
        textElement.setText(text);
        updateTextElement();
        if (textElement.getParent() == null) {
            this.attachChild(textElement);
        }
    }

    /**
     * Returns the current visible text of the element.
     *
     * @return String text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a pointer to the BitmapText element of this Element.  Returns null if setText() has
     * not been called.
     *
     * @return BitmapText textElement
     */
    public BitmapText getTextElement() {
        return textElement;
    }

    /**
     * Updates font materials with any changes to clipping layers
     */
    private void setFontPages() {
        if (textElement != null) {
            if (!isVisible) {
                for (int i = 0; i < font.getPageSize(); i++) {
                    this.font.getPage(i).setVector4("Clipping", clippingBounds);
                    this.font.getPage(i).setBoolean("UseClipping", true);
                }
            } else {
                if (isClipped) {
                    for (int i = 0; i < font.getPageSize(); i++) {
                        this.font.getPage(i).setVector4("Clipping", clippingBounds.add(textClipPadding.x, textClipPadding.y, -textClipPadding.z, -textClipPadding.w));
                        this.font.getPage(i).setBoolean("UseClipping", true);
                    }
                } else {
                    for (int i = 0; i < font.getPageSize(); i++) {
                        this.font.getPage(i).setBoolean("UseClipping", false);
                    }
                }
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Materials, Textures & Atlas">
    private void throwParserException() {
        try {
            throw new java.text.ParseException("The provided texture information does not conform to the expected standard of x=(int)|y=(int)|w=(int)|h=(int)", 0);
        } catch (ParseException ex) {
            Logger.getLogger(Element.class.getName()).log(Level.SEVERE, "The provided texture information does not conform to the expected standard of x=(int)|y=(int)|w=(int)|h=(int)", ex);
        }
    }

    /**
     * Sets the texture to use as an atlas image as well as the atlas image coords.
     *
     * @param texture     The texture to use as a local atlas image
     * @param queryString The position of the desire atlas image (e.g. "x=0|y=0|w=50|h=50")
     */
    public void setTextureAtlasImage(Texture texture, String queryString) {
        useLocalTexture = false;
        defaultTexture = texture;

        material.setTexture(PARAM_COLOR_MAP, texture);
        material.setColor(PARAM_COLOR, new ColorRGBA(1, 1, 1, 1));
        material.setBoolean(PARAM_USE_EFFECT_TEX_COORDS, true);

        this.useLocalAtlas = true;
        this.atlasCoords = queryString;

        float[] coords = screen.parseAtlasCoords(queryString);
        float textureAtlasX = coords[0];
        float textureAtlasY = coords[1];
        float textureAtlasW = coords[2];
        float textureAtlasH = coords[3];

        final Image image = defaultTexture.getImage();

        float imgWidth = image.getWidth();
        float imgHeight = image.getHeight();
        float pixelWidth = 1f / imgWidth;
        float pixelHeight = 1f / imgHeight;

        textureAtlasY = imgHeight - textureAtlasY - textureAtlasH;

        model = new ElementQuadGrid(dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, textureAtlasX, textureAtlasY, textureAtlasW, textureAtlasH);
        geom.setMesh(model);
    }

    /**
     * Returns the current unparsed string representing the Element's atlas image
     */
    public String getAtlasCoords() {
        return this.atlasCoords;
    }

    /**
     * Sets the element image to the specified x/y/width/height
     *
     * @param queryString (e.g. "x=0|y=0|w=50|h=50")
     */
    public void updateTextureAtlasImage(String queryString) {

        float[] coords = screen.parseAtlasCoords(queryString);

        float textureAtlasX = coords[0];
        float textureAtlasY = coords[1];
        float textureAtlasW = coords[2];
        float textureAtlasH = coords[3];

        float imgWidth = defaultTexture.getImage().getWidth();
        float imgHeight = defaultTexture.getImage().getHeight();

        float pixelWidth = 1f / imgWidth;
        float pixelHeight = 1f / imgHeight;

        textureAtlasY = imgHeight - textureAtlasY - textureAtlasH;

        final ElementQuadGrid model = getModel();
        model.updateTexCoords(textureAtlasX, textureAtlasY, textureAtlasW, textureAtlasH);
    }

    /**
     * Returns if the element is using a local texture atlas of the screen defined texture atlas
     */
    public boolean getUseLocalAtlas() {
        return useLocalAtlas;
    }

    /**
     * Returns the difference between the placement of the elements current image and the given
     * texture coords.
     *
     * @param coords The x/y coords of the new image
     * @return Vector2f containing The difference between the given coords and the original image
     */
    public Vector2f getAtlasTextureOffset(float[] coords) {

        Texture texture;

        if (defaultTexture != null) texture = defaultTexture;
        else texture = screen.getAtlasTexture();

        final Image image = texture.getImage();

        float imgWidth = image.getWidth();
        float imgHeight = image.getHeight();
        float pixelWidth = 1f / imgWidth;
        float pixelHeight = 1f / imgHeight;

        return new Vector2f(getModel().getEffectOffset(pixelWidth * coords[0], pixelHeight * (imgHeight - coords[1] - coords[3])));
    }

    public void setUseLocalTexture(boolean useLocalTexture) {
        this.useLocalTexture = useLocalTexture;
    }

    public boolean getUseLocalTexture() {
        return this.useLocalTexture;
    }

    /**
     * Will set the textures WrapMode to repeat if enabled.<br/><br/> NOTE: This only works when
     * texture atlasing has not been enabled. For info on texture atlas usage, see both:<br/>
     *
     * @see Screen#setUseTextureAtlas(boolean enable, String path)
     * @see #setTextureAtlasImage(com.jme3.texture.Texture tex, java.lang.String path)
     */
    public void setTileImage(boolean tileImage) {
        this.useLocalTexture = true;
        this.tileImage = tileImage;

        if (tileImage) {
            ((Texture) material.getParam("ColorMap").getValue()).setWrap(Texture.WrapMode.Repeat);
        } else {
            ((Texture) material.getParam("ColorMap").getValue()).setWrap(Texture.WrapMode.Clamp);
        }

        setDimensions(dimensions);
    }

    public boolean getTileImage() {
        return this.tileImage;
    }

    public void setTileImageByKey(String style, String key) {
        boolean tile = false;
        try {
            tile = screen.getStyle(style).getBoolean(key);
        } catch (Exception ex) {
        }
        setTileImage(tile);
    }

    public void setClipPaddingByKey(String style, String key) {
        try {
            setClipPadding(screen.getStyle(style).getFloat(key));
        } catch (Exception ex) {
        }
        try {
            setClipPadding(screen.getStyle(style).getVector4f(key));
        } catch (Exception ex) {
        }
    }

    public void setTextPaddingByKey(String style, String key) {
        try {
            setTextPadding(screen.getStyle(style).getFloat(key));
        } catch (Exception ex) {
        }
        try {
            setTextPadding(screen.getStyle(style).getVector4f(key));
        } catch (Exception ex) {
        }
    }

    public void setTextClipPaddingByKey(String style, String key) {
        try {
            setTextClipPadding(screen.getStyle(style).getFloat(key));
        } catch (Exception ex) {
        }
        try {
            setTextClipPadding(screen.getStyle(style).getVector4f(key));
        } catch (Exception ex) {
        }
    }

    /**
     * Returns the default material for the element
     *
     * @return Material material
     */
    public Material getMaterial() {
        return this.material;
    }

    /**
     * A way to override the default material of the element.
     *
     * NOTE: It is important that the shader used with the new material is either: A: The provided
     * Unshaded material contained with this library, or B: The custom shader contains the caret,
     * text range, clipping and effect handling provided in the default shader.
     *
     * @param mat The Material to use for rendering this Element.
     */
    public void setLocalMaterial(Material mat) {
        this.material = mat;
        this.setMaterial(mat);
    }

    /**
     * Returns the default material for the element
     */
    public void setElementMaterial(Material mat) {
        this.material = mat;
    }

    /**
     * Returns a pointer to the Material used for rendering this Element.
     *
     * @return Material material
     */
    public Material getElementMaterial() {
        return this.material;
    }

    /**
     * Returns the default Texture for the Element
     *
     * @return Texture defaultTexture
     */
    public Texture getElementTexture() {
        return defaultTexture;
    }

    /**
     * Adds an alpha map to the Elements material
     *
     * @param texturePath A String path to the alpha map
     */
    public void setAlphaTexture(final String texturePath) {

        final AssetManager assetManager = app.getAssetManager();

        Texture texture = null;

        if (screen.getUseTextureAtlas() && !useLocalTexture) {
            if (this.getElementTexture() != null) texture = getElementTexture();
            else texture = screen.getAtlasTexture();
            final Vector2f alphaOffset = getAtlasTextureOffset(screen.parseAtlasCoords(texturePath));
            material.setVector2(PARAM_OFFSET_ALPHA_TEX_COORD, alphaOffset);
        } else {
            texture = assetManager.loadTexture(texturePath);
            texture.setMinFilter(Texture.MinFilter.NearestLinearMipMap);
            texture.setMagFilter(Texture.MagFilter.Bilinear);
            texture.setWrap(Texture.WrapMode.EdgeClamp);
        }

        this.alphaMap = texture;

        final Image image = texture.getImage();

        if (defaultTexture == null) {
            if (!screen.getUseTextureAtlas() || useLocalTexture) {

                float imgWidth = image.getWidth();
                float imgHeight = image.getHeight();
                float pixelWidth = 1f / imgWidth;
                float pixelHeight = 1f / imgHeight;

                this.model = new ElementQuadGrid(dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, 0, 0, imgWidth, imgHeight);
                geom.setMesh(model);

            } else {

                float[] coords = screen.parseAtlasCoords(texturePath);
                float textureAtlasX = coords[0];
                float textureAtlasY = coords[1];
                float textureAtlasW = coords[2];
                float textureAtlasH = coords[3];

                float imgWidth = image.getWidth();
                float imgHeight = image.getHeight();
                float pixelWidth = 1f / imgWidth;
                float pixelHeight = 1f / imgHeight;

                textureAtlasY = imgHeight - textureAtlasY - textureAtlasH;

                model = new ElementQuadGrid(dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, textureAtlasX, textureAtlasY, textureAtlasW, textureAtlasH);
                geom.setMesh(model);

                material.setVector2(PARAM_OFFSET_ALPHA_TEX_COORD, new Vector2f(0, 0));
            }
        }

        material.setTexture(PARAM_ALPHA_MAP, texture);
    }

    public Texture getAlphaMap() {
        return alphaMap;
    }

    public void setBackgroundTexture(final String texturePath) {

        final AssetManager assetManager = app.getAssetManager();

        Texture texture = null;

        if (screen.getUseTextureAtlas() && !useLocalTexture) {
            if (getElementTexture() != null) texture = getElementTexture();
            else texture = screen.getAtlasTexture();
        } else {
            texture = assetManager.loadTexture(texturePath);
            texture.setMinFilter(Texture.MinFilter.NearestLinearMipMap);
            texture.setMagFilter(Texture.MagFilter.Bilinear);
            texture.setWrap(Texture.WrapMode.EdgeClamp);
        }

        this.defaultTexture = texture;

        final Image image = texture.getImage();

        if (!screen.getUseTextureAtlas() || useLocalTexture) {

            float imgWidth = image.getWidth();
            float imgHeight = image.getHeight();
            float pixelWidth = 1f / imgWidth;
            float pixelHeight = 1f / imgHeight;

            this.model = new ElementQuadGrid(dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, 0, 0, imgWidth, imgHeight);
            geom.setMesh(model);

        } else {

            float[] coords = screen.parseAtlasCoords(texturePath);

            float textureAtlasX = coords[0];
            float textureAtlasY = coords[1];
            float textureAtlasW = coords[2];
            float textureAtlasH = coords[3];

            float imgWidth = image.getWidth();
            float imgHeight = image.getHeight();
            float pixelWidth = 1f / imgWidth;
            float pixelHeight = 1f / imgHeight;

            textureAtlasY = imgHeight - textureAtlasY - textureAtlasH;

            model = new ElementQuadGrid(dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, textureAtlasX, textureAtlasY, textureAtlasW, textureAtlasH);
            geom.setMesh(model);
        }

        material.setTexture(PARAM_COLOR_MAP, texture);
        material.setColor(PARAM_COLOR, ColorRGBA.White);
    }

    public void rebuildModel() {
        if (!screen.getUseTextureAtlas() || useLocalTexture) {
            float imgWidth = defaultTexture.getImage().getWidth();
            float imgHeight = defaultTexture.getImage().getHeight();
            float pixelWidth = 1f / imgWidth;
            float pixelHeight = 1f / imgHeight;

            this.model = new ElementQuadGrid(this.dimensions, borders, imgWidth, imgHeight, pixelWidth, pixelHeight, 0, 0, imgWidth, imgHeight);
            geom.setMesh(model);
        } else {
            float[] coords = screen.parseAtlasCoords(this.atlasCoords);
            float textureAtlasX = coords[0];
            float textureAtlasY = coords[1];
            float textureAtlasW = coords[2];
            float textureAtlasH = coords[3];

            float imgWidth = defaultTexture.getImage().getWidth();
            float imgHeight = defaultTexture.getImage().getHeight();
            float pixelWidth = 1f / imgWidth;
            float pixelHeight = 1f / imgHeight;

            textureAtlasY = imgHeight - textureAtlasY - textureAtlasH;

            model = new ElementQuadGrid(this.getDimensions(), borders, imgWidth, imgHeight, pixelWidth, pixelHeight, textureAtlasX, textureAtlasY, textureAtlasW, textureAtlasH);
            geom.setMesh(model);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Visibility">

    /**
     * This may be remove soon and probably should not be used as the method of handling hide show
     * was updated making this unnecissary.
     *
     * @param wasVisible boolean
     */
    public void setDefaultWasVisible(boolean wasVisible) {
        this.wasVisible = wasVisible;
    }

    /**
     * Shows the current Element with the defined Show effect.  If no Show effect is defined, the
     * Element will show as normal.
     */
    public void showWithEffect() {
        Effect effect = getEffect(Effect.EffectEvent.Show);
        if (effect != null) {
            if (effect.getEffectType() == Effect.EffectType.FadeIn) {
                Effect clone = effect.clone();
                clone.setAudioFile(null);
                this.propagateEffect(clone, false);
            } else {
                if (getTextElement() != null)
                    getTextElement().setAlpha(1f);
                screen.getEffectManager().applyEffect(effect);
            }
        } else
            this.show();
    }

    /**
     * Sets this Element and any Element contained within it's nesting order to visible.
     *
     * NOTE: Hide and Show relies on shader-based clipping
     */
    public void show() {
        if (isVisible) return;

        this.isVisible = true;
        this.isClipped = wasClipped;

        //	updateClipping();
        updateClippingLayers();
        controlShowHook();

        final BitmapText textElement = getTextElement();
        if (textElement != null) textElement.setAlpha(1F);

        final Node parent = getParent();
        final Element elementParent = getElementParent();

        if (parent == null) {
            if (elementParent != null) elementParent.attachChild(this);
            else screen.getGUINode().attachChild(this);
        }

        elementChildren.values().forEach(Element::childShow);
    }

    public void showAsModal(boolean showWithEffect) {
        isVisibleAsModal = true;
        screen.showAsModal(this, showWithEffect);
    }

    /**
     * Recursive call for properly showing children of the Element.  I'm thinking this this needs to
     * be a private method, however I need to verify this before I update it.
     */
    public void childShow() {

        final BitmapText textElement = getTextElement();
        if (textElement != null) textElement.setAlpha(1F);

        this.isVisible = wasVisible;
        this.isClipped = wasClipped;

        //	updateClipping();
        updateClippingLayers();
        controlShowHook();

        elementChildren.values().forEach(Element::childShow);
    }

    /**
     * An overridable method for extending the show event.
     */
    public void controlShowHook() {
    }

    /**
     * Hides the element using the current defined Hide effect.  If no Hide effect is defined, the
     * Element will hide as usual.
     */
    public void hideWithEffect() {
        Effect effect = getEffect(Effect.EffectEvent.Hide);
        if (effect != null) {
            if (effect.getEffectType() == Effect.EffectType.FadeOut) {
                Effect clone = effect.clone();
                clone.setAudioFile(null);
                this.propagateEffect(clone, true);
            } else
                screen.getEffectManager().applyEffect(effect);
            if (isVisibleAsModal) {
                isVisibleAsModal = false;
                screen.hideModalBackground();
            }
        } else
            this.hide();
    }

    /**
     * Recursive call that sets this Element and any Element contained within it's nesting order to
     * hidden.
     *
     * NOTE: Hide and Show relies on shader-based clipping
     */
    public void hide() {

        if (isVisible()) {

            if (isVisibleAsModal) {
                isVisibleAsModal = false;
                screen.hideModalBackground();
            }

            this.wasVisible = isVisible;
            this.isVisible = false;
            this.isClipped = true;

            if (screen.getUseToolTips() && screen.getToolTipFocus() == this) {
                screen.hideToolTip();
            }
        }

        //	updateClipping();
        updateClippingLayers();
        controlHideHook();
        removeFromParent();

        elementChildren.values().forEach(Element::childHide);
    }

    /**
     * For internal use.  This method should never be called directly.
     */
    public void childHide() {

        if (isVisible()) {
            this.wasVisible = isVisible;
            this.isVisible = false;
            this.isClipped = true;

            if (screen.getUseToolTips() && screen.getToolTipFocus() == this) {
                screen.hideToolTip();
            }
        }

        //	updateClipping();
        updateClippingLayers();
        controlHideHook();

        elementChildren.values().forEach(Element::childHide);
    }

    /**
     * Hides or shows the element (true = show, false = hide)
     */
    public void setVisible(boolean visible) {
        if (visible) show();
        else hide();
    }

    /**
     * Toggles the Element's visibility based on the current state.
     */
    public void switchVisible() {
        if (isVisible()) hide();
        else show();
    }

    /**
     * An overridable method for extending the hide event.
     */
    public void controlHideHook() {
    }

    /**
     * Return if the Element is visible
     *
     * @return boolean isVisible
     */
    public boolean isVisible() {
        return isVisible;
    }

    //</editor-fold>

    //<editor-fold desc="Cleanup">
    public void cleanup() {
        controlCleanupHook();
        elementChildren.values().forEach(Element::cleanup);
    }

    /**
     * An overridable method for handling control specific cleanup.
     */
    public void controlCleanupHook() {
    }
    //</editor-fold>

    //<editor-fold desc="Clipping">

    /**
     * Sets the elements clipping layer to the provided element.
     *
     * @param clippingLayer The element that provides the clipping boundaries.
     */
    @Deprecated
    public void setClippingLayer(Element clippingLayer) {
        if (clippingLayer != null)
            addClippingLayer(clippingLayer);
        else
            removeClippingLayer(clippingLayer);
        /*
        if (clippingLayer != null) {
			this.isClipped = true;
			this.wasClipped = true;
			this.clippingLayer = clippingLayer;
			this.material.setBoolean("UseClipping", true);
		} else {
			this.isClipped = false;
			this.wasClipped = false;
			this.clippingLayer = null;
			this.material.setBoolean("UseClipping", false);
		}
		*/
    }

    /**
     * Sets the elements clipping layer to the provided element.
     *
     * @param clippingLayer The element that provides the clipping boundaries.
     */
    @Deprecated
    public void setSecondaryClippingLayer(Element secondaryClippingLayer) {
        if (secondaryClippingLayer != null)
            addClippingLayer(secondaryClippingLayer);
        else
            removeClippingLayer(secondaryClippingLayer);
        /*
        if (secondaryClippingLayer != null) {
			this.secondaryClippingLayer = secondaryClippingLayer;
		} else {
			this.secondaryClippingLayer = null;
		}
		*/
    }

    /**
     * Recursive update of all child Elements clipping layer
     *
     * @param clippingLayer The clipping layer to apply
     */
    @Deprecated
    public void setControlClippingLayer(Element clippingLayer) {
        setClippingLayer(clippingLayer);
        //	for (Element el : elementChildren.values()) {
        //		el.setControlClippingLayer(clippingLayer);
        //	}
    }

    /**
     * Recursive update of all child Elements clipping & secondary clipping layers
     *
     * @param clippingLayer          The clipping layer to apply
     * @param secondaryClippingLayer The clipping layer's parent clipping layer to apply
     */
    @Deprecated
    public void setControlClippingLayer(Element clippingLayer, Element secondaryClippingLayer) {
        setClippingLayer(clippingLayer);
        setSecondaryClippingLayer(secondaryClippingLayer);
        for (Element el : elementChildren.values()) {
            el.setControlClippingLayer(clippingLayer, secondaryClippingLayer);
        }
    }

    /**
     * Returns if the Element's clipping layer has been set
     *
     * @return boolean isClipped
     */
    public boolean getIsClipped() {
        return isClipped;
    }

    /**
     * Returns the elements clipping layer or null is element doesn't use clipping
     *
     * @return Element clippingLayer
     */
    public Element getClippingLayer() {
        return this.clippingLayer;
    }

    /**
     * Returns a Vector4f containing the current boundaries of the element's clipping layer
     *
     * @return Vector4f clippingBounds
     */
    public Vector4f getClippingBounds() {
        return this.clippingBounds;
    }

    /**
     * Adds a padding to the clippinglayer, in effect this contracts the size of the clipping bounds
     * by the specified number of pixels
     *
     * @param clipPadding The number of pixels to pad the clipping area
     */
    public void setClipPadding(float clipPadding) {
        this.clipPadding.set(
                clipPadding, clipPadding, clipPadding, clipPadding
        );
    }

    public void setClipPadding(float clipLeft, float clipRight, float clipTop, float clipBottom) {
        this.clipPadding.set(
                clipLeft, clipTop, clipRight, clipBottom
        );
    }

    public void setClipPadding(Vector4f clipPadding) {
        this.clipPadding.set(
                clipPadding
        );
    }

    /**
     * Returns the current clipPadding
     *
     * @return float clipPadding
     */
    public float getClipPadding() {
        return clipPadding.x;
    }

    public Vector4f getClipPaddingVec() {
        return clipPadding;
    }

    /**
     * Shrinks the clipping area by set number of pixels
     *
     * @param textClipPadding The number of pixels to pad the clipping area with on each side
     */
    public void setTextClipPadding(float textClipPadding) {
        this.textClipPadding.set(
                textClipPadding, textClipPadding, textClipPadding, textClipPadding
        );
    }

    public void setTextClipPadding(float clipLeft, float clipRight, float clipTop, float clipBottom) {
        this.textClipPadding.set(
                clipLeft, clipTop, clipRight, clipBottom
        );
    }

    public void setTextClipPadding(Vector4f textClipPadding) {
        this.textClipPadding.set(
                textClipPadding
        );
    }

    public float getTextClipPadding() {
        return textClipPadding.x;
    }

    public Vector4f getTextClipPaddingVec() {
        return textClipPadding;
    }

    /**
     * Updates the clipping bounds for any element that has a clipping layer
     *
     * See updateLocalClipping
     */
    @Deprecated
    public void updateClipping() {
        //	updateLocalClipping();
        updateLocalClippingLayer();
        for (Element el : elementChildren.values()) {
            el.updateClipping();
        }
    }

    /**
     * Updates the clipping bounds for any element that has a clipping layer
     */
    @Deprecated
    public void updateLocalClipping() {
        if (isVisible) {
            if (clippingLayer != null) {
                if (secondaryClippingLayer == null) {
                    clippingBounds.set(
                            clippingLayer.getAbsoluteX() + clipPadding.x,
                            clippingLayer.getAbsoluteY() + clipPadding.y,
                            clippingLayer.getAbsoluteWidth() - clipPadding.z,
                            clippingLayer.getAbsoluteHeight() - clipPadding.w
                    );
                } else {
                    float clX = clippingLayer.getAbsoluteX();
                    float sclX = secondaryClippingLayer.getAbsoluteX();
                    float clY = clippingLayer.getAbsoluteY();
                    float sclY = secondaryClippingLayer.getAbsoluteY();
                    float clW = clippingLayer.getAbsoluteWidth();
                    float sclW = secondaryClippingLayer.getAbsoluteWidth();
                    float clH = clippingLayer.getAbsoluteHeight();
                    float sclH = secondaryClippingLayer.getAbsoluteHeight();

                    clippingBounds.set(
                            (clX > sclX) ? clX + clipPadding.x : sclX + clipPadding.x,
                            (clY > sclY) ? clY + clipPadding.y : sclY + clipPadding.y,
                            (clW < sclW) ? clW - clipPadding.z : sclW - clipPadding.z,
                            (clH < sclH) ? clH - clipPadding.w : sclH - clipPadding.w
                    );
                }
                material.setVector4("Clipping", clippingBounds);
                if (!(Boolean) material.getParam("UseClipping").getValue())
                    material.setBoolean("UseClipping", true);
            } else {
                if ((Boolean) material.getParam("UseClipping").getValue())
                    material.setBoolean("UseClipping", false);
            }
        } else {
            clippingBounds.set(0, 0, 0, 0);
            material.setVector4("Clipping", clippingBounds);
            if (!(Boolean) material.getParam("UseClipping").getValue())
                material.setBoolean("UseClipping", true);
        }
        setFontPages();
    }

    // New Clipping
    public void addClippingLayer(Element el) {
        ClippingDefine def = new ClippingDefine(el);
        propigateClippingLayerAdd(def);
    }

    public void addClippingLayer(Element el, Vector4f relativeClippingBounds) {
        ClippingDefine def = new ClippingDefine(el, relativeClippingBounds);
        propigateClippingLayerAdd(def);
    }

    public ClippingDefine getClippingDefine(Element el) {
        ClippingDefine def = null;

        for (ClippingDefine d : clippingLayers) {
            if (d.getElement() == el) {
                def = d;
                break;
            }
        }
        return def;
    }

    public void updateClippingLayer(Element el, Vector4f clip) {
        ClippingDefine def = getClippingDefine(el);
        if (def != null) {
            if (def.clip == null) def.clip = new Vector4f();
            def.clip.set(clip);
        }
        validateClipSettings();
        for (Element c : elementChildren.values()) {
            c.updateClippingLayer(el, clip);
        }
    }

    public void propigateClippingLayerAdd(ClippingDefine def) {
        if (!clippingLayers.contains(def))
            clippingLayers.add(def);
        validateClipSettings();
        for (Element c : elementChildren.values()) {
            c.propigateClippingLayerAdd(def);
        }
    }

    public void removeClippingLayer(Element el) {
        for (ClippingDefine def : clippingLayers) {
            if (def.getElement() == el)
                remClippingLayers.add(def);
        }
        if (!remClippingLayers.isEmpty()) {
            clippingLayers.removeAll(remClippingLayers);
            remClippingLayers.clear();
        }
        for (Element c : elementChildren.values()) {
            c.removeClippingLayer(el);
        }
    }

    public boolean getHasClippingLayers() {
        return !clippingLayers.isEmpty();
    }

    public void updateClippingLayers() {
        updateLocalClippingLayer();
        validateClipSettings();
        for (Element c : elementChildren.values()) {
            c.updateClippingLayers();
        }
    }

    public void updateLocalClippingLayer() {
        if (isVisible) {
            if (!clippingLayers.isEmpty()) {
                calcClipping();
                setFontPages();
            }
        }
    }

    private void calcClipping() {
        float cX = 0;
        float cY = 0;
        float cW = screen.getWidth();
        float cH = screen.getHeight();

        for (ClippingDefine def : clippingLayers) {
            clipTest.set(def.getClipping());
            if (def.getElement() != this) {
                clipTest.addLocal(
                        def.getElement().getClipPaddingVec().x,
                        def.getElement().getClipPaddingVec().y,
                        -def.getElement().getClipPaddingVec().z,
                        -def.getElement().getClipPaddingVec().w
                );
            }
            if (clipTest.x > cX) cX = clipTest.x;
            if (clipTest.y > cY) cY = clipTest.y;
            if (clipTest.z < cW) cW = clipTest.z;
            if (clipTest.w < cH) cH = clipTest.w;
        }
        clippingBounds.set(cX, cY, cW, cH);
    }

    protected void validateClipSettings() {
        if (!clippingLayers.isEmpty()) {
            this.isClipped = true;
            this.wasClipped = true;
            if (!(Boolean) material.getParam("UseClipping").getValue())
                material.setBoolean("UseClipping", true);
        } else {
            this.isClipped = false;
            this.wasClipped = false;
            if ((Boolean) material.getParam("UseClipping").getValue())
                material.setBoolean("UseClipping", false);
        }
        material.setVector4("Clipping", clippingBounds);
    }

    public class ClippingDefine {
        public Element owner;
        public Vector4f clip = null;
        private Vector4f tempV4 = new Vector4f();

        public ClippingDefine(Element owner) {
            this.owner = owner;
        }

        public ClippingDefine(Element owner, Vector4f clip) {
            this.owner = owner;
            this.clip = new Vector4f(clip);
        }

        public Element getElement() {
            return owner;
        }

        public Vector4f getClipping() {
            if (clip == null) {
                tempV4.setX(owner.getAbsoluteX());
                tempV4.setY(owner.getAbsoluteY());
                tempV4.setZ(tempV4.getX() + owner.getWidth());
                tempV4.setW(tempV4.getY() + owner.getHeight());
            } else {
                float x = owner.getAbsoluteX();
                float y = owner.getAbsoluteY();
                tempV4.set(
                        x + clip.x,
                        y + clip.y,
                        x + clip.z,
                        y + clip.w
                );
            }
            return tempV4;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Effects">

    /**
     * Associates an Effect with this Element.  Effects are not automatically associated with the
     * specified event, but instead, the event type is used to retrieve the Effect at a later point
     *
     * @param effectEvent The Effect.EffectEvent the Effect is to be registered with
     * @param effect      The Effect to store
     */
    public void addEffect(Effect.EffectEvent effectEvent, Effect effect) {
        addEffect(effect);
    }

    /**
     * Associates an Effect with this Element.  Effects are not automatically associated with the
     * specified event, but instead, the event type is used to retrieve the Effect at a later point
     *
     * @param effect The Effect to store
     */
    public void addEffect(Effect effect) {
        effects.remove(effect.getEffectEvent());
        if (!effects.containsKey(effect.getEffectEvent())) {
            effect.setElement(this);
            effects.put(effect.getEffectEvent(), effect);
        }
    }

    /**
     * Removes the Effect associated with the Effect.EffectEvent specified
     */
    public void removeEffect(Effect.EffectEvent effectEvent) {
        effects.remove(effectEvent);
    }

    /**
     * Retrieves the Effect associated with the specified Effect.EffectEvent
     *
     * @return effect
     */
    public Effect getEffect(Effect.EffectEvent effectEvent) {
        Effect effect = null;
        if (effects.get(effectEvent) != null)
            effect = effects.get(effectEvent).clone();
        return effect;
    }

    /**
     * Called by controls during construction to prepopulate effects based on Styles.
     *
     * @param styleName The String identifier of the Style
     */
    protected void populateEffects(String styleName) {
        int index = 0;
        Effect effect;
        while ((effect = screen.getStyle(styleName).getEffect("event" + index)) != null) {
            effect = effect.clone();
            effect.setElement(this);
            this.addEffect(effect);
            index++;
        }
    }

    /**
     * For internal use only - DO NOT CALL THIS METHOD
     *
     * @param effect   Effect
     * @param callHide boolean
     */
    public void propagateEffect(Effect effect, boolean callHide) {
        Effect nEffect = effect.clone();
        nEffect.setCallHide(callHide);
        nEffect.setElement(this);
        screen.getEffectManager().applyEffect(nEffect);
        for (Element el : elementChildren.values()) {
            el.propagateEffect(effect, false);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Alpha">

    /**
     * Overrides the screen global alpha with the specified value. setIngoreGlobalAlpha must be
     * enabled prior to calling this method.
     */
    public void setGlobalAlpha(float globalAlpha) {
        if (!ignoreGlobalAlpha) {
            getElementMaterial().setFloat(PARAM_GLOBAL_ALPHA, globalAlpha);
            for (Element el : elementChildren.values()) {
                el.setGlobalAlpha(globalAlpha);
            }
        } else {
            getElementMaterial().setFloat(PARAM_GLOBAL_ALPHA, 1);
        }
    }

    /**
     * Will enable or disable the use of the screen defined global alpha setting.
     */
    public void setIgnoreGlobalAlpha(boolean ignoreGlobalAlpha) {
        this.ignoreGlobalAlpha = ignoreGlobalAlpha;
    }
    //</editor-fold>

    //<editor-fold desc="Focus">

    /**
     * For use by the Form control (Do not call this method directly)
     *
     * @param form The form the Element has been added to
     */
    public void setForm(Form form) {
        this.form = form;
    }

    /**
     * Returns the form the Element is controlled by
     *
     * @return Form form
     */
    public Form getForm() {
        return this.form;
    }

    /**
     * Sets the tab index (This is assigned by the Form control. Do not call this method directly)
     *
     * @param tabIndex The tab index assigned to the Element
     */
    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    /**
     * Returns the tab index assigned by the Form control
     *
     * @return tabIndex
     */
    public int getTabIndex() {
        return tabIndex;
    }


    /**
     * For internal use - DO NOT CALL THIS METHOD
     *
     * @param hasFocus boolean
     */
    public void setHasFocus(boolean hasFocus) {
        this.hasFocus = hasFocus;
    }

    /**
     * Returns if the Element currently has input focus
     */
    public boolean getHasFocus() {
        return this.hasFocus;
    }

    public void setResetKeyboardFocus(boolean resetKeyboardFocus) {
        this.resetKeyboardFocus = resetKeyboardFocus;
    }

    public boolean getResetKeyboardFocus() {
        return this.resetKeyboardFocus;
    }

    //</editor-fold>

    // Off Screen Rendering Bridge
    public void addOSRBridge(OSRBridge bridge) {
        this.bridge = bridge;
        addControl(bridge);
        getElementMaterial().setTexture("ColorMap", bridge.getTexture());
        getElementMaterial().setColor("Color", ColorRGBA.White);
    }

    //<editor-fold desc="Tool Tips">

    /**
     * Sets the Element's ToolTip text
     *
     * @param toolTip String
     */
    public void setToolTipText(String toolTip) {
        this.toolTipText = toolTip;
    }

    /**
     * Returns the Element's current ToolTip text
     *
     * @return String
     */
    public String getToolTipText() {
        return toolTipText;
    }
    //</editor-fold>

    //<editor-fold desc="Modal">

    /**
     * Enables standard modal mode for the Element.
     */
    public void setIsModal(boolean isModal) {
        this.isModal = isModal;
    }

    /**
     * Returns if the Element is currently modal
     *
     * @return Ret
     */
    public boolean getIsModal() {
        return this.isModal;
    }

    /**
     * For internal use - DO NOT CALL THIS METHOD
     *
     * @param hasFocus boolean
     */
    public void setIsGlobalModal(boolean isGlobalModal) {
        this.isGlobalModal = isGlobalModal;
    }

    /**
     * For internal use - DO NOT CALL THIS METHOD
     *
     * @param hasFocus boolean
     */
    public boolean getIsGlobalModal() {
        return this.isGlobalModal;
    }
    //</editor-fold>

    //<editor-fold desc="User Data">

    /**
     * Stores provided data with the Element
     *
     * @param elementUserData Object Data to store
     */
    public void setElementUserData(Object elementUserData) {
        this.elementUserData = elementUserData;
    }

    /**
     * Returns the data stored with this Element
     *
     * @return Object
     */
    public Object getElementUserData() {
        return this.elementUserData;
    }
    //</editor-fold>

    Vector2f origin = new Vector2f(0, 0);

    /**
     * Stubbed for future use
     */
    public void setOrigin(float originX, float originY) {
        origin.set(originX, originY);
    }

    /**
     * Stubbed for future use.
     */
    public Vector2f getOrigin() {
        return this.origin;
    }

    //<editor-fold desc="Layouts">
    public void setLayout(Layout layout) {
        this.layout = layout;
        this.layout.setOwner(this);
    }

    public Layout getLayout() {
        return this.layout;
    }

    public void setLayoutHints(LayoutHints layoutHints) {
        this.layoutHints = layoutHints;
    }

    public LayoutHints getLayoutHints() {
        return this.layoutHints;
    }

    public void layoutChildren() {
        if (layout != null) {
            layout.layoutChildren();
        }
        for (Element el : elementChildren.values()) {
            el.layoutChildren();
        }
    }
    //</editor-fold>
}
