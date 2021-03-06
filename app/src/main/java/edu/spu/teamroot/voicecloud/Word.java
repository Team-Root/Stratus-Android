package edu.spu.teamroot.voicecloud;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class Word extends WordGroup {
    private String name;
    private int count;
    private long timestamp;

    private AnimatorSet animatorSet;

    public Button button;
    public RelativeLayout.LayoutParams layoutParams;

    public Word(String name, int count) {
        super();

        init(name, count);
        createButton();
    }

    /*
     * Methods
     */

    // Initializes the word attributes.
    private void init(String name, int count) {
        Log.d(name, "init(" + count + ")");

        this.name = name;
        this.count = (count >= 0) ? count : 0;
        this.timestamp = System.currentTimeMillis();
    }

    // Removes the word from the word list.
    // Also detaches from the view and removes from word tree.
    public void delete() {
        Log.d(name, "delete(" + count + ")");

        if (isAttached()) {
            detachFromCloud();
        }

        if (isCreated()) {
            if (animatorSet != null) {
                animatorSet.cancel();
            }

            destroyButton();
        }
    }

    // Creates the word button.
    private void createButton() {
        Log.d(name, "createButton(" + count + ")");

        if (!isCreated()) {
            button = new Button(WordCloud.context);
            button.setVisibility(View.INVISIBLE);

            button.setText(name.toUpperCase());
            button.setTextColor(WordCloud.context.getResources().getColor(android.R.color.white));

            // If older than Lollipop, use custom button Drawable
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                button.setTypeface(null, Typeface.BOLD);
                button.setBackground(WordCloud.context.getResources().getDrawable(R.drawable.word_drawable));
            }

            setColor(WordCloud.getInstance().weighter.getWordColor(this));

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    wordActions(Word.this);
                }
            });

            // Create layoutParams (not used until word attached)
            layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = 0;

            // Set initial size (do not animate)
            refreshSize(false);
        }
    }

    // Destroys the word button.
    private void destroyButton() {
        Log.d(name, "destroyButton(" + count + ")");

        if (isCreated()) {
            button = null;
            layoutParams = null;
        }
    }

    // Attaches the word to the word cloud.
    // Alias for WordCloud.attachWord().
    private void attachToCloud() {
        Log.d(name, "attachToCloud(" + count + ")");

        if (!isAttached()) {
            WordCloud.getInstance().attachWord(this);
        }
    }

    // Detaches the word from the word cloud.
    // Alias for WordCloud.detachWord().
    private void detachFromCloud() {
        Log.d(name, "detachFromCloud(" + count + ")");

        if (isAttached()) {
            WordCloud.getInstance().detachWord(this);
        }
    }

    // Shows the word on the view. Animation depends on if the button is already visible.
    public void show() {
        show(button.getVisibility() == View.INVISIBLE);
    }

    // Shows the word on the view.
    public void show(boolean animate) {
        Log.d(name, "show(" + count + ")");

        if (!isCreated()) return;

        button.setVisibility(View.VISIBLE);

        if (isAttached() && animate) {
            ScaleAnimation anim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            anim.setInterpolator(new SpringInterpolator());

            anim.setDuration(1000);

            button.clearAnimation();
            button.setAnimation(anim);
            button.animate();
        }
    }

    // Hides the word from the view. Animation depends on if the button is already invisible.
    public void hide() {
        hide(button.getVisibility() == View.VISIBLE);
    }

    // Hides the word from the view.
    public void hide(boolean animate) {
        Log.d(name, "hide(" + count + ")");

        if (!isCreated()) return;

        button.setVisibility(View.INVISIBLE);

        if (isAttached() && animate) {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setInterpolator(new LinearInterpolator());

            anim.setDuration(500);

            button.clearAnimation();
            button.setAnimation(anim);
            button.animate();
        }
    }

    // Returns true if the button has been created.
    public boolean isCreated() {
        return button != null;
    }

    // Returns true if the button is attached to the word cloud.
    public boolean isAttached() {
        return parent != null;
    }

    // Returns the name of the word.
    public String getName() {
        return name;
    }

    // Returns the occurrence count.
    public int getCount() {
        return count;
    }

    // Returns the timestamp of the last occurrence.
    public long getTimestamp() {
        return timestamp;
    }

    // Increments the count by the value, and updates the size.
    public boolean incrementCount(int value) {
        // Increment count
        count += value;

        // Update timestamp
        this.timestamp = System.currentTimeMillis();

        if (count <= 0) {
            return false;
        }

        // Button size has changed; animate
        refreshSize(true);

        return true;
    }

    // Refreshes the size of the word based on the current count and weighting.
    private void refreshSize(boolean animate) {
        if (!isCreated()) return;

        // Cache old size
        Rect oldBounds = new Rect(bounds);

        Log.d(name, "Left: " + UnitConverter.getInstance().toDp(button.getLeft()) + " Top: " + UnitConverter.getInstance().toDp(button.getTop()));

        // Calculate new size based on count
        button.setTextSize(WordCloud.getInstance().weighter.getTextSize(this));
        Log.d(name, "Count: " + count + " Size: " + WordCloud.getInstance().weighter.getTextSize(this));

        // Calculate new color based on count (or part of speech)
        setColor(WordCloud.getInstance().weighter.getWordColor(this));

        // Update bounds with new button size
        button.measure(WordCloud.layout.getWidth(), WordCloud.layout.getHeight());

        bounds.set(bounds.left, bounds.top,
                bounds.left + UnitConverter.getInstance().toDp(button.getMeasuredWidth()),
                bounds.top + UnitConverter.getInstance().toDp(button.getMeasuredHeight()));

        /* Extra checks to see if the button Width and Height are available... to my knowledge, they are always incorrect...
        if (button.getWidth() == 0 && button.getHeight() == 0) {
            // The button has likely not been drawn in the View yet...
            // We need to fallback and get a calculated size based on the layout
            button.measure(WordCloud.layout.getWidth(), WordCloud.layout.getHeight());

            bounds.set(bounds.left, bounds.top,
                    bounds.left + UnitConverter.getInstance().toDp(button.getMeasuredWidth()),
                    bounds.top + UnitConverter.getInstance().toDp(button.getMeasuredHeight()));
        } else {
            bounds.set(bounds.left, bounds.top,
                    bounds.left + UnitConverter.getInstance().toDp(button.getWidth()),
                    bounds.top + UnitConverter.getInstance().toDp(button.getHeight()));
        }
        //*/

        Log.d(name, "Bounds: " + bounds.toString());

        center.x = bounds.centerX();
        center.y = bounds.centerY();

        Log.d(name, "Center: " + center.toString());

        if (isAttached() && animate) {
            float prevX = (float) oldBounds.width() / bounds.width();
            float prevY = (float) oldBounds.height() / bounds.height();

            ScaleAnimation anim = new ScaleAnimation(prevX, 1.0f, prevY, 1.0f);
            anim.setInterpolator(new SpringInterpolator());

            anim.setDuration(500);

            button.clearAnimation();
            button.setAnimation(anim);
            button.animate();
        }
    }

    // Sets the color of the word button.
    private void setColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.getBackground().setColorFilter(new LightingColorFilter(0xFF000000, color));
        } else {
            // Compatible buttons are solid white, so multiply color
            button.getBackground().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }
    }

    @Override
    public void moveBy(int dx, int dy) {
        // Only animate if the button is visible, otherwise, just move it
        moveBy(dx, dy, button.getVisibility() == View.VISIBLE);
    }

    // Moves the word by a specified delta.
    private void moveBy(int dx, int dy, boolean animate) {
        center.offset(dx, dy);
        bounds.offset(dx, dy);

        // Cancel existing animation (we are moving the word again)
        if (animatorSet != null) {
            if (animatorSet.isStarted()) {
                animatorSet.cancel();
            }
        }

        if (isAttached() && animate) {
            // Create animators (using updated destination value)
            ValueAnimator xAnim = new MarginAnimationListener(
                    WordCloud.layout, layoutParams, MarginAnimationListener.LEFT_MARGIN,
                    UnitConverter.getInstance().toPx(bounds.left)).getValueAnimator();
            ValueAnimator yAnim = new MarginAnimationListener(
                    WordCloud.layout, layoutParams, MarginAnimationListener.TOP_MARGIN,
                    UnitConverter.getInstance().toPx(bounds.top)).getValueAnimator();

            // Combine animators into one set
            animatorSet = new AnimatorSet();

            animatorSet.playTogether(xAnim, yAnim);
            animatorSet.setInterpolator(new SpringInterpolator());
            animatorSet.setDuration(1000);

            animatorSet.start();
        } else {
            layoutParams.leftMargin = UnitConverter.getInstance().toPx(bounds.left);
            layoutParams.topMargin = UnitConverter.getInstance().toPx(bounds.top);

            WordCloud.layout.requestLayout();
        }

        Log.d(name, "moveBy: (" + dx + "," + dy + ") Bounds: " + bounds.toString() + " Center: " + center.toString());
    }

    @Override
    public void moveTo(int x, int y) {
        // Only animate if the button is visible, otherwise, just move it
        moveTo(x, y, button.getVisibility() == View.VISIBLE);
    }

    // Moves the word to a specified point.
    private void moveTo(int x, int y, boolean animate) {
        int dx = x - center.x;
        int dy = y - center.y;

        moveBy(dx, dy, animate);
    }

    @Override
    public String toString() {
        return name;
    }

    // Shows the list of word actions.
    private void wordActions(final Word word) {

        final ListPopupWindow popupWindow = new ListPopupWindow(WordCloud.context);
        //String wordName = word.getName().toUpperCase();

        // ListPopupWindow Dimensions
        // DP to Pixel
        int dpsHeight = 230;
        int dpsWidth = 200;
        float scale = WordCloud.context.getResources().getDisplayMetrics().density;

        // Set Dimensions
        popupWindow.setHeight((int)(dpsHeight * scale + 0.5f));
        popupWindow.setWidth((int) (dpsWidth * scale + 0.5f));
        popupWindow.setAnchorView(button);

        ArrayList<HashMap<String, String>> dataList = new ArrayList<>();
        HashMap<String, String> curItemMap;

        curItemMap = new HashMap<>();
        curItemMap.put("id", "count");
        //curItemMap.put("icon", String.valueOf(R.mipmap.green_icon));
        curItemMap.put("iconText", Integer.toString(word.getCount()));
        curItemMap.put("label", "Count");
        dataList.add(curItemMap);

        curItemMap = new HashMap<>();
        double countPerMinute = word.getCount() / ((System.currentTimeMillis() - WordCloud.getInstance().getTimestamp()) / 60000.0); // Find count per minute
        //curItemMap.put("icon", String.valueOf(R.mipmap.green_icon));
        curItemMap.put("iconText", countPerMinute >= 10 ? String.format("%d", (int) countPerMinute) : String.format("%.1f", countPerMinute));
        curItemMap.put("label", "Per Minute");
        dataList.add(curItemMap);

        curItemMap = new HashMap<>();
        curItemMap.put("icon", String.valueOf(R.mipmap.google_icon));
        curItemMap.put("iconText", "");
        curItemMap.put("label", "Google");
        dataList.add(curItemMap);

        curItemMap = new HashMap<>();
        curItemMap.put("icon", String.valueOf(R.mipmap.wikipedia_icon));
        curItemMap.put("iconText", "");
        curItemMap.put("label", "Wikipedia");
        dataList.add(curItemMap);

        curItemMap = new HashMap<>();
        curItemMap.put("icon", String.valueOf(R.mipmap.dictionary_icon));
        curItemMap.put("iconText", "");
        curItemMap.put("label", "Define");
        dataList.add(curItemMap);

        curItemMap = new HashMap<>();
        curItemMap.put("icon", String.valueOf(R.mipmap.remove_icon));
        curItemMap.put("iconText", "");
        curItemMap.put("label", "Remove/Exclude");
        dataList.add(curItemMap);

        SimpleAdapter simpleAdapter = new SimpleAdapter(WordCloud.context, dataList, R.layout.word_action_row, new String[]{"icon", "iconText", "label"}, new int[]{R.id.icon, R.id.iconText, R.id.label});

        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Count
                if (position == 0) { }

                // Count per minute
                if (position == 1) { }

                // Search with Google
                if (position == 2) {
                    String url = "http://www.google.com/search?q=" + word.getName();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    WordCloud.context.startActivity(i);
                }

                // Lookup on Wikipedia
                if (position == 3) {
                    String url = "http://wikipedia.org/wiki/" + word.getName();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    WordCloud.context.startActivity(i);
                }

                if (position == 4) {
                    String url = "http://dictionary.reference.com/browse/" + word.getName();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    WordCloud.context.startActivity(i);
                }

                // Remove from Word Cloud
                if (position == 5) {
                    // Remove word from cloud
                    WordCloud.getInstance().removeWord(word);

                    // Add word to exclusion list
                    ExclusionList.getInstance().addWord(word.getName());

                    popupWindow.dismiss();
                }
            }
        };

        popupWindow.setAdapter(simpleAdapter);
        popupWindow.setOnItemClickListener(listener);

        popupWindow.show();
    }

}