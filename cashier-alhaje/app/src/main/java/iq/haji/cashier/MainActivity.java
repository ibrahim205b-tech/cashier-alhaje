import android.app.role.RoleManager;
package iq.haji.cashier;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.telecom.TelecomManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private static final String PIN = "8876";
    private static final String PREF = "haji_cashier";
    private static final String DATA = "cashier_data_v1";

    private final int BG = 0xFFF4F6F8;
    private final int NAVY = 0xFF202A36;
    private final int ORANGE = 0xFFF4A62A;
    private final int ORANGE_DARK = 0xFFD97706;
    private final int WHITE = 0xFFFFFFFF;
    private final int GREEN = 0xFF16845B;
    private final int RED = 0xFFCE3E42;
    private final int MUTED = 0xFF667085;
    private final int BORDER = 0xFFE0E5EB;

    private SharedPreferences prefs;

    private LinearLayout root;
    private LinearLayout categoriesBox;
    private LinearLayout dishesBox;
    private LinearLayout cartBox;
    private LinearLayout topBar;

    private TextView totalText;
    private TextView numberHint;
    private TextView orderNumberText;

    private EditText phoneInput;
    private EditText addressInput;

    private Spinner typeSpinner;
    private Spinner driverSpinner;

    private final ArrayList<Category> categories = new ArrayList<>();
    private final ArrayList<Dish> dishes = new ArrayList<>();
    private final ArrayList<CartItem> cart = new ArrayList<>();
    private final ArrayList<String> drivers = new ArrayList<>();
    private final ArrayList<JSONObject> orders = new ArrayList<>();

    private String selectedCategory = "";

    private int nextOrderNo = 1;
    private int completedCount = 0;
    private int cancelledCount = 0;
    private int callsCount = 0;

    private long seenCallTime = 0;
    private boolean printing = false;

    static class Category {
        String name;

        Category(String n) {
            name = n;
        }
    }

    static class Dish {
        String name;
        String category;
        int price;

        Dish(String n, String c, int p) {
            name = n;
            category = c;
            price = p;
        }
    }

    static class CartItem {
        String name;
        String category;
        int price;
        int qty;

        CartItem(String n, String c, int p, int q) {
            name = n;
            category = c;
            price = p;
            qty = q;
        }

        int total() {
            return price * qty;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        prefs = getSharedPreferences(PREF, MODE_PRIVATE);

        loadData();
        showLogin();
private void requestCallScreeningRole() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return;
    }

    RoleManager roleManager =
            (RoleManager) getSystemService(Context.ROLE_SERVICE);

    if (roleManager == null) {
        return;
    }

    if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
        return;
    }

    if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
        return;
    }

    Intent intent = roleManager.createRequestRoleIntent(
            RoleManager.ROLE_CALL_SCREENING
    );

    startActivityForResult(intent, 1001);
}
    private TextView text(
            String s,
            int size,
            int color,
            boolean bold
    ) {
        TextView t = new TextView(this);

        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(null, 1);
        }

        t.setGravity(Gravity.CENTER_VERTICAL);

        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private GradientDrawable bg(
            int color,
            int radius,
            int stroke
    ) {
        GradientDrawable d = new GradientDrawable();

        d.setColor(color);
        d.setCornerRadius(dp(radius));

        if (stroke != 0) {
            d.setStroke(dp(1), stroke);
        }

        return d;
    }

    private TextView button(
            String label,
            int color,
            int textColor,
            Runnable action
    ) {
        TextView b = text(label, 15, textColor, true);

        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setBackground(bg(color, 12, 0));

        b.setOnClickListener(v -> action.run());
        b.setClickable(true);

        return b;
    }

    private void addWeighted(
            LinearLayout parent,
            View child,
            float weight
    ) {
        parent.addView(
                child,
                new LinearLayout.LayoutParams(0, -1, weight)
        );
    }

    private void showLogin() {
        LinearLayout wrap = column();

        wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(dp(24), dp(24), dp(24), dp(24));
        wrap.setBackgroundColor(BG);

        TextView logo = text("👨‍🍳", 56, NAVY, true);
        logo.setGravity(Gravity.CENTER);

        wrap.addView(
                logo,
                new LinearLayout.LayoutParams(-1, dp(76))
        );

        TextView title = text("مطعم الحجي", 30, NAVY, true);
        title.setGravity(Gravity.CENTER);

        wrap.addView(
                title,
                new LinearLayout.LayoutParams(-1, dp(52))
        );

        TextView sub = text("نظام الكاشير", 18, MUTED, false);
        sub.setGravity(Gravity.CENTER);

        wrap.addView(
                sub,
                new LinearLayout.LayoutParams(-1, dp(40))
        );

        EditText pin = new EditText(this);

        pin.setSingleLine(true);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(24);

        pin.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        pin.setHint("أدخل الرمز");
        pin.setBackground(bg(WHITE, 14, BORDER));
        pin.setPadding(dp(16), dp(8), dp(16), dp(8));

        LinearLayout.LayoutParams ep =
                new LinearLayout.LayoutParams(dp(300), dp(58));

        ep.setMargins(0, dp(18), 0, dp(12));
        wrap.addView(pin, ep);

        TextView enter = button(
                "دخول",
                ORANGE,
                NAVY,
                () -> {
                    if (PIN.equals(pin.getText().toString())) {
                        showCashier();
                    } else {
                        pin.setError("الرمز غير صحيح");
                        pin.setText("");
                    }
                }
        );

        wrap.addView(
                enter,
                new LinearLayout.LayoutParams(dp(300), dp(54))
        );

        setContentView(wrap);
    }

    private void showCashier() {
        root = column();
        root.setBackgroundColor(BG);

        topBar = row();
        topBar.setPadding(dp(16), dp(8), dp(16), dp(8));
        topBar.setBackgroundColor(NAVY);

        LinearLayout brand = column();

        brand.addView(
                text("👨‍🍳  مطعم الحجي", 22, WHITE, true)
        );

        brand.addView(
                text(
                        "نظام الكاشير • يعمل بدون إنترنت",
                        12,
                        0xFFCED5DE,
                        false
                )
        );

        topBar.addView(
                brand,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        topBar.addView(
                button(
                        "📊 التقارير",
                        0xFF354354,
                        WHITE,
                        this::showReports
                )
        );

        topBar.addView(spaceW(8));

        topBar.addView(
                button(
                        "⚙ إدارة",
                        0xFF354354,
                        WHITE,
                        this::showManagement
                )
        );

        topBar.addView(spaceW(8));

        topBar.addView(
                button("خروج", RED, WHITE, this::showLogin)
        );

        root.addView(
                topBar,
                new LinearLayout.LayoutParams(-1, -2)
        );

        LinearLayout body = row();

        body.setPadding(dp(10), dp(10), dp(10), dp(10));
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        categoriesBox = column();
        categoriesBox.setPadding(dp(8), dp(8), dp(8), dp(8));
        categoriesBox.setBackground(bg(WHITE, 16, BORDER));

        LinearLayout.LayoutParams catLp =
                new LinearLayout.LayoutParams(0, -1, 1.0f);

        catLp.setMargins(dp(4), 0, dp(4), 0);
        body.addView(categoriesBox, catLp);

        dishesBox = column();
        dishesBox.setPadding(dp(8), dp(8), dp(8), dp(8));
        dishesBox.setBackground(bg(BG, 16, 0));

        LinearLayout.LayoutParams dishLp =
                new LinearLayout.LayoutParams(0, -1, 1.65f);

        dishLp.setMargins(dp(4), 0, dp(4), 0);
        body.addView(dishesBox, dishLp);

        LinearLayout orderPanel = column();

        orderPanel.setPadding(dp(10), dp(10), dp(10), dp(10));
        orderPanel.setBackground(bg(WHITE, 16, BORDER));

        LinearLayout.LayoutParams orderLp =
                new LinearLayout.LayoutParams(0, -1, 1.5f);

        orderLp.setMargins(dp(4), 0, dp(4), 0);
        body.addView(orderPanel, orderLp);

        root.addView(
                body,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        TextView orderTitle = text("الطلب الحالي", 20, NAVY, true);

        orderPanel.addView(
                orderTitle,
                new LinearLayout.LayoutParams(-1, dp(36))
        );

        orderNumberText = text(
                "الفاتورة القادمة  #" + nextOrderNo,
                13,
                MUTED,
                true
        );

        orderPanel.addView(
                orderNumberText,
                new LinearLayout.LayoutParams(-1, dp(25))
        );

        LinearLayout details = row();

        typeSpinner = new Spinner(this);

        String[] types = {"دليفري", "سفري", "محلي"};

        typeSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        types
                )
        );

        details.addView(
                typeSpinner,
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        driverSpinner = new Spinner(this);
        refreshDriverSpinner();

        details.addView(
                driverSpinner,
                new LinearLayout.LayoutParams(0, dp(46), 1)
        );

        orderPanel.addView(details);

        phoneInput = new EditText(this);

        phoneInput.setSingleLine(true);
        phoneInput.setTextSize(15);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setHint("رقم الزبون — يظهر عند بدء الطلب");
        phoneInput.setPadding(dp(10), 0, dp(10), 0);
        phoneInput.setBackground(bg(BG, 10, BORDER));

        orderPanel.addView(
                phoneInput,
                new LinearLayout.LayoutParams(-1, dp(46))
        );

        LinearLayout.LayoutParams gap =
                new LinearLayout.LayoutParams(-1, dp(7));

        orderPanel.addView(new View(this), gap);

        addressInput = new EditText(this);

        addressInput.setSingleLine(true);
        addressInput.setTextSize(14);
        addressInput.setHint("العنوان / ملاحظة (اختياري)");
        addressInput.setPadding(dp(10), 0, dp(10), 0);
        addressInput.setBackground(bg(BG, 10, BORDER));

        orderPanel.addView(
                addressInput,
                new LinearLayout.LayoutParams(-1, dp(42))
        );

        ScrollView cartScroll = new ScrollView(this);

        cartBox = column();
        cartBox.setPadding(0, dp(6), 0, dp(6));
        cartScroll.addView(cartBox);

        LinearLayout.LayoutParams scrollLp =
                new LinearLayout.LayoutParams(-1, 0, 1);

        scrollLp.setMargins(0, dp(8), 0, dp(4));
        orderPanel.addView(cartScroll, scrollLp);

        totalText = text("المجموع: 0 د.ع", 24, GREEN, true);

        totalText.setGravity(Gravity.CENTER);
        totalText.setBackground(bg(0xFFE7F6EE, 12, 0));

        orderPanel.addView(
                totalText,
                new LinearLayout.LayoutParams(-1, dp(52))
        );

        LinearLayout actions = row();

        actions.addView(
                button(
                        "إلغاء الطلب",
                        0xFFFFE8E8,
                        RED,
                        this::cancelOrder
                ),
                new LinearLayout.LayoutParams(0, dp(48), 1)
        );

        actions.addView(spaceW(8));

        actions.addView(
                button(
                        "طباعة وحفظ الفاتورة",
                        ORANGE_DARK,
                        WHITE,
                        this::saveAndPrint
                ),
                new LinearLayout.LayoutParams(0, dp(48), 1.4f)
        );

        LinearLayout.LayoutParams actLp =
                new LinearLayout.LayoutParams(-1, -2);

        actLp.setMargins(0, dp(8), 0, 0);
        orderPanel.addView(actions, actLp);

        setContentView(root);

        renderCategories();
        renderDishes();
        renderCart();

        getWindow().getDecorView().post(this::updateIncomingNumber);

        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateIncomingNumber();

                if (root != null) {
                    root.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private View spaceW(int widthDp) {
        View v = new View(this);

        v.setLayoutParams(
                new LinearLayout.LayoutParams(dp(widthDp), 1)
        );

        return v;
    }

    private void renderCategories() {
        categoriesBox.removeAllViews();

        TextView head = text("الأقسام", 18, NAVY, true);

        categoriesBox.addView(
                head,
                new LinearLayout.LayoutParams(-1, dp(40))
        );

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = column();

        scroll.addView(list);

        for (Category c : categories) {
            TextView b = button(
                    (c.name.equals(selectedCategory) ? "●  " : "") + c.name,
                    c.name.equals(selectedCategory) ? ORANGE : BG,
                    NAVY,
                    () -> {
                        selectedCategory = c.name;
                        renderCategories();
                        renderDishes();
                    }
            );

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(-1, dp(52));

            lp.setMargins(0, dp(3), 0, dp(3));
            list.addView(b, lp);
        }

        categoriesBox.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        categoriesBox.addView(
                button(
                        "+ إضافة قسم",
                        0xFFFFF1D5,
                        ORANGE_DARK,
                        this::addCategoryDialog
                ),
                new LinearLayout.LayoutParams(-1, dp(46))
        );
    }

    private void renderDishes() {
        dishesBox.removeAllViews();

        LinearLayout head = row();

        head.addView(
                text("الأطباق", 20, NAVY, true),
                new LinearLayout.LayoutParams(0, dp(40), 1)
        );

        head.addView(
                button("+ طبق", ORANGE, NAVY, this::addDishDialog)
        );

        dishesBox.addView(head);

        ScrollView scroll = new ScrollView(this);
        LinearLayout grid = column();

        scroll.addView(grid);

        LinearLayout current = null;
        int count = 0;

        for (Dish d : dishes) {
            if (!d.category.equals(selectedCategory)) {
                continue;
            }

            if (count % 2 == 0) {
                current = row();

                grid.addView(
                        current,
                        new LinearLayout.LayoutParams(-1, dp(100))
                );
            }

            final Dish dish = d;

            LinearLayout card = column();

            card.setPadding(dp(10), dp(8), dp(10), dp(8));
            card.setGravity(Gravity.CENTER);
            card.setBackground(bg(WHITE, 14, BORDER));

            TextView name = text(d.name, 16, NAVY, true);

            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);

            card.addView(
                    name,
                    new LinearLayout.LayoutParams(-1, 0, 1)
            );

            TextView price = text(
                    String.format(Locale.US, "%,d د.ع", d.price),
                    15,
                    ORANGE_DARK,
                    true
            );

            price.setGravity(Gravity.CENTER);

            card.addView(
                    price,
                    new LinearLayout.LayoutParams(-1, dp(25))
            );

            card.setOnClickListener(v -> addDishToCart(dish));

            card.setOnLongClickListener(v -> {
                editDishDialog(dish);
                return true;
            });

            LinearLayout.LayoutParams cp =
                    new LinearLayout.LayoutParams(0, -1, 1);

            cp.setMargins(dp(3), dp(3), dp(3), dp(3));

            current.addView(card, cp);

            count++;
        }

        if (count == 0) {
            TextView empty = text(
                    "ماكو أطباق بهذا القسم\nاضغط + طبق حتى تضيف",
                    16,
                    MUTED,
                    false
            );

            empty.setGravity(Gravity.CENTER);

            grid.addView(
                    empty,
                    new LinearLayout.LayoutParams(-1, dp(150))
            );
        }

        dishesBox.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        dishesBox.addView(
                text(
                        "اضغط على الطبق للإضافة • ضغطة مطوّلة لتعديل السعر",
                        11,
                        MUTED,
                        false
                ),
                new LinearLayout.LayoutParams(-1, dp(25))
        );
    }

    private void addDishToCart(Dish d) {
        if (cart.isEmpty()) {
            String latest = prefs.getString("latest_number", "");

            if (!latest.isEmpty()) {
                phoneInput.setText(latest);
            }
        }

        CartItem found = null;

        for (CartItem i : cart) {
            if (i.name.equals(d.name) && i.price == d.price) {
                found = i;
                break;
            }
        }

        if (found == null) {
            cart.add(new CartItem(d.name, d.category, d.price, 1));
        } else {
            found.qty++;
        }

        renderCart();
    }

    private void renderCart() {
        if (cartBox == null) {
            return;
        }

        cartBox.removeAllViews();

        if (cart.isEmpty()) {
            TextView empty = text(
                    "السلة فارغة\nاختار الأطباق حتى يبدأ الطلب",
                    14,
                    MUTED,
                    false
            );

            empty.setGravity(Gravity.CENTER);

            cartBox.addView(
                    empty,
                    new LinearLayout.LayoutParams(-1, dp(100))
            );
        }

        for (CartItem item : cart) {
            LinearLayout r = row();

            r.setPadding(dp(5), dp(4), dp(5), dp(4));
            r.setBackground(bg(BG, 10, 0));

            LinearLayout info = column();

            info.addView(text(item.name, 14, NAVY, true));

            info.addView(
                    text(
                            String.format(
                                    Locale.US,
                                    "%d × %,d = %,d",
                                    item.qty,
                                    item.price,
                                    item.total()
                            ),
                            12,
                            MUTED,
                            false
                    )
            );

            r.addView(
                    info,
                    new LinearLayout.LayoutParams(0, -2, 1)
            );

            r.addView(
                    button(
                            "−",
                            0xFFE5EAF0,
                            NAVY,
                            () -> {
                                item.qty--;

                                if (item.qty <= 0) {
                                    cart.remove(item);
                                }

                                renderCart();
                            }
                    ),
                    new LinearLayout.LayoutParams(dp(38), dp(38))
            );

            r.addView(spaceW(4));

            r.addView(
                    button(
                            "+",
                            0xFFFFF1D5,
                            ORANGE_DARK,
                            () -> {
                                item.qty++;
                                renderCart();
                            }
                    ),
                    new LinearLayout.LayoutParams(dp(38), dp(38))
            );

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(-1, -2);

            lp.setMargins(0, dp(3), 0, dp(3));

            cartBox.addView(r, lp);
        }

        int total = cartTotal();

        totalText.setText("المجموع: " + money(total));

        orderNumberText.setText(
                "الفاتورة القادمة  #" + nextOrderNo +
                "  •  " + cart.size() + " أصناف"
        );
    }

    private int cartTotal() {
        int total = 0;

        for (CartItem i : cart) {
            total += i.total();
        }

        return total;
    }

    private String money(int n) {
        return String.format(Locale.US, "%,d د.ع", n);
    }

    private void updateIncomingNumber() {
        if (phoneInput == null || isFinishing()) {
            return;
        }

        String latest = prefs.getString("latest_number", "");
        long time = prefs.getLong("latest_call_time", 0);

        if (time > seenCallTime) {
            seenCallTime = time;
            callsCount++;
            saveData();
        }

        if (!cart.isEmpty() &&
                time > 0 &&
                time >= seenCallTime &&
                latest != null &&
                !latest.isEmpty()) {

            String existing = phoneInput.getText().toString();

            if (!latest.equals(existing)) {
                phoneInput.setText(latest);
            }
        }
    }

    private void addCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("اسم القسم");

        new AlertDialog.Builder(this)
                .setTitle("إضافة قسم")
                .setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    String n = input.getText().toString().trim();

                    if (!n.isEmpty()) {
                        categories.add(new Category(n));
                        selectedCategory = n;

                        saveData();
                        renderCategories();
                        renderDishes();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void addDishDialog() {
        LinearLayout form = column();

        form.setPadding(dp(12), dp(4), dp(12), 0);

        EditText name = new EditText(this);
        name.setHint("اسم الطبق");
        form.addView(name);

        EditText price = new EditText(this);
        price.setHint("السعر بالدينار");
        price.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(price);

        Spinner cat = new Spinner(this);

        ArrayList<String> names = new ArrayList<>();

        for (Category c : categories) {
            names.add(c.name);
        }

        cat.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        names.toArray(new String[0])
                )
        );

        form.addView(cat);

        new AlertDialog.Builder(this)
                .setTitle("إضافة طبق")
                .setView(form)
                .setPositiveButton("حفظ", (d, w) -> {
                    try {
                        String n = name.getText().toString().trim();

                        int p = Integer.parseInt(
                                price.getText().toString().trim()
                        );

                        String c = cat.getSelectedItem().toString();

                        if (!n.isEmpty() && p >= 0) {
                            dishes.add(new Dish(n, c, p));
                            selectedCategory = c;

                            saveData();
                            renderCategories();
                            renderDishes();
                        }
                    } catch (Exception e) {
                        toast("تأكد من اسم الطبق والسعر");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void editDishDialog(Dish dish) {
        LinearLayout form = column();

        form.setPadding(dp(12), 0, dp(12), 0);

        EditText name = new EditText(this);
        name.setText(dish.name);
        form.addView(name);

        EditText price = new EditText(this);
        price.setText(String.valueOf(dish.price));
        price.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(price);

        new AlertDialog.Builder(this)
                .setTitle("تعديل الطبق")
                .setMessage("تعديل الاسم أو السعر. للحذف اضغط حذف.")
                .setView(form)
                .setPositiveButton("حفظ", (d, w) -> {
                    try {
                        String n = name.getText().toString().trim();

                        int p = Integer.parseInt(
                                price.getText().toString().trim()
                        );

                        if (!n.isEmpty() && p >= 0) {
                            dish.name = n;
                            dish.price = p;

                            saveData();
                            renderDishes();
                        }
                    } catch (Exception e) {
                        toast("السعر غير صحيح");
                    }
                })
                .setNeutralButton("حذف", (d, w) ->
                        new AlertDialog.Builder(this)
                                .setMessage("تأكيد حذف " + dish.name + "؟")
                                .setPositiveButton("حذف", (x, y) -> {
                                    dishes.remove(dish);
                                    saveData();
                                    renderDishes();
                                })
                                .setNegativeButton("رجوع", null)
                                .show()
                )
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void cancelOrder() {
        if (cart.isEmpty()) {
            toast("السلة فارغة");
            return;
        }

        EditText reason = new EditText(this);
        reason.setHint("سبب الإلغاء");

        new AlertDialog.Builder(this)
                .setTitle("إلغاء الطلب")
                .setMessage("سيتم تسجيل الطلب الملغي بالتقرير")
                .setView(reason)
                .setPositiveButton("تأكيد الإلغاء", (d, w) -> {
                    JSONObject o = makeOrder("ملغي");

                    try {
                        o.put(
                                "cancel_reason",
                                reason.getText().toString()
                        );
                    } catch (Exception ignored) {
                    }

                    orders.add(o);
                    cancelledCount++;

                    clearOrder();
                    saveData();

                    toast("تم تسجيل الإلغاء");
                })
                .setNegativeButton("رجوع", null)
                .show();
    }

    private JSONObject makeOrder(String status) {
        JSONObject o = new JSONObject();

        try {
            o.put("number", nextOrderNo);

            o.put(
                    "time",
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                    ).format(new Date())
            );

            o.put("status", status);

            o.put(
                    "type",
                    typeSpinner == null
                            ? "دليفري"
                            : typeSpinner.getSelectedItem().toString()
            );

            o.put(
                    "phone",
                    phoneInput == null
                            ? ""
                            : phoneInput.getText().toString()
            );

            o.put(
                    "address",
                    addressInput == null
                            ? ""
                            : addressInput.getText().toString()
            );

            o.put(
                    "driver",
                    driverSpinner == null
                            ? ""
                            : driverSpinner.getSelectedItem().toString()
            );

            JSONArray arr = new JSONArray();

            for (CartItem i : cart) {
                JSONObject x = new JSONObject();

                x.put("name", i.name);
                x.put("category", i.category);
                x.put("price", i.price);
                x.put("qty", i.qty);
                x.put("total", i.total());

                arr.put(x);
            }

            o.put("items", arr);
            o.put("total", cartTotal());

        } catch (Exception ignored) {
        }

        return o;
    }

    private void saveAndPrint() {
        if (cart.isEmpty()) {
            toast("أضف طبق واحد على الأقل قبل الطباعة");
            return;
        }

        JSONObject order = makeOrder("مكتمل");

        orders.add(order);
        completedCount++;
        nextOrderNo++;

        saveData();

        printing = true;

        printHtml(
                buildReceipt(order),
                "فاتورة مطعم الحجي #" + (nextOrderNo - 1)
        );

        clearOrder();

        toast("تم حفظ الفاتورة. أكمل الطباعة من نافذة الطابعة");
    }

    private void clearOrder() {
        cart.clear();

        if (phoneInput != null) {
            phoneInput.setText("");
        }

        if (addressInput != null) {
            addressInput.setText("");
        }

        if (cartBox != null) {
            renderCart();
        }

        prefs.edit()
                .remove("latest_number")
                .remove("latest_call_time")
                .apply();

        seenCallTime = 0;
    }

    private String buildReceipt(JSONObject o) {
        StringBuilder s = new StringBuilder();

        s.append("<html><head><meta charset='utf-8'><style>")
                .append("@page{size:58mm auto;margin:2mm}")
                .append("body{font-family:sans-serif;width:54mm;margin:0;color:#000;font-size:10pt;direction:rtl}")
                .append("h2{text-align:center;font-size:15pt;margin:3px 0}")
                .append("p{margin:3px 0}")
                .append("table{width:100%;border-collapse:collapse;font-size:9pt}")
                .append("th,td{padding:3px 1px;border-bottom:1px dashed #555;text-align:center}")
                .append("hr{border:0;border-top:1px dashed #000}")
                .append(".total{text-align:center;font-size:14pt;font-weight:bold;border:1px solid #000;padding:6px;margin-top:5px}")
                .append("</style></head><body><h2>مطعم الحجي</h2>")
                .append("<p style='text-align:center'>فاتورة ")
                .append(esc(o.optString("type", "دليفري")))
                .append("</p><hr>")
                .append("<p>رقم الفاتورة: #")
                .append(o.optInt("number"))
                .append("</p><p>التاريخ: ")
                .append(esc(o.optString("time")))
                .append("</p>");

        if (!o.optString("phone").isEmpty()) {
            s.append("<p>رقم الزبون: ")
                    .append(esc(o.optString("phone")))
                    .append("</p>");
        }

        if (!o.optString("driver").isEmpty()) {
            s.append("<p>السائق: ")
                    .append(esc(o.optString("driver")))
                    .append("</p>");
        }

        s.append("<hr><table>")
                .append("<tr><th>ت</th><th>اسم المادة</th>")
                .append("<th>الكمية</th><th>السعر</th>")
                .append("<th>اجمالي</th></tr>");

        JSONArray arr = o.optJSONArray("items");

        int idx = 1;
        int qty = 0;

        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject x = arr.optJSONObject(i);

                if (x == null) {
                    continue;
                }

                qty += x.optInt("qty");

                s.append("<tr><td>")
                        .append(idx++)
                        .append("</td><td>")
                        .append(esc(x.optString("name")))
                        .append("</td><td>")
                        .append(x.optInt("qty"))
                        .append("</td><td>")
                        .append(x.optInt("price"))
                        .append("</td><td>")
                        .append(x.optInt("total"))
                        .append("</td></tr>");
            }
        }

        s.append("</table><hr><p>الكمية: ")
                .append(qty)
                .append("　　 اجمالي المبلغ</p>")
                .append("<div class='total'>المبلغ الصافي<br>")
                .append(String.format(Locale.US, "%,d", o.optInt("total")))
                .append(" د.ع</div>")
                .append("<p style='text-align:center;margin-top:8px'>شكراً لطلبكم</p>")
                .append("</body></html>");

        return s.toString();
    }

    private String esc(String v) {
        if (v == null) {
            return "";
        }

        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void printHtml(String html, String jobName) {
        PrintManager pm = (PrintManager)
                getSystemService(Context.PRINT_SERVICE);

        if (pm == null) {
            toast("خدمة الطباعة غير متوفرة بالجهاز");
            return;
        }

        WebView web = new WebView(this);

        web.getSettings().setDefaultTextEncodingName("UTF-8");

        web.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintDocumentAdapter adapter =
                        view.createPrintDocumentAdapter(jobName);

                PrintAttributes attrs =
                        new PrintAttributes.Builder()
                                .setMediaSize(
                                        new PrintAttributes.MediaSize(
                                                "ROLL_58",
                                                "58 mm",
                                                2283,
                                                100000
                                        )
                                )
                                .setResolution(
                                        new PrintAttributes.Resolution(
                                                "thermal",
                                                "Thermal",
                                                203,
                                                203
                                        )
                                )
                                .setMinMargins(
                                        PrintAttributes.Margins.NO_MARGINS
                                )
                                .build();

                pm.print(jobName, adapter, attrs);
            }
        });

        web.loadDataWithBaseURL(
                null,
                html,
                "text/HTML",
                "UTF-8",
                null
        );
    }

    private void showManagement() {
        String[] options = {
                "إضافة طبق / تعديل الأسعار",
                "إضافة سائق",
                "إدارة الأقسام",
                "اختيار الطابعة (إعدادات أندرويد)",
                "تصدير تقرير اليوم"
        };

        new AlertDialog.Builder(this)
                .setTitle("إدارة الكاشير")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        addDishDialog();
                    } else if (which == 1) {
                        addDriverDialog();
                    } else if (which == 2) {
                        addCategoryDialog();
                    } else if (which == 3) {
                        Intent i = new Intent(
                                "android.settings.BLUETOOTH_SETTINGS"
                        );

                        startActivity(i);
                    } else {
                        exportReport();
                    }
                })
                .show();
    }

    private void addDriverDialog() {
        EditText input = new EditText(this);
        input.setHint("اسم السائق");

        new AlertDialog.Builder(this)
                .setTitle("إضافة سائق")
                .setView(input)
                .setPositiveButton("إضافة", (d, w) -> {
                    String n = input.getText().toString().trim();

                    if (!n.isEmpty()) {
                        drivers.add(n);
                        saveData();
                        refreshDriverSpinner();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void refreshDriverSpinner() {
        if (driverSpinner == null) {
            return;
        }

        ArrayList<String> list = new ArrayList<>();

        list.add("بدون سائق");
        list.addAll(drivers);

        driverSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        list
                )
        );
    }

    private void showReports() {
        int totalSales = 0;
        int todayOrders = 0;
        int todayCancelled = 0;

        String today = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());

        HashMap<String, Integer> popular = new HashMap<>();
        HashMap<String, Integer> categoryTotals = new HashMap<>();

        for (JSONObject o : orders) {
            if (!o.optString("time").startsWith(today)) {
                continue;
            }

            if ("مكتمل".equals(o.optString("status"))) {
                todayOrders++;
                totalSales += o.optInt("total");
            }

            if ("ملغي".equals(o.optString("status"))) {
                todayCancelled++;
            }

            JSONArray a = o.optJSONArray("items");

            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject it = a.optJSONObject(i);

                    if (it == null) {
                        continue;
                    }

                    String nm = it.optString("name");

                    popular.put(
                            nm,
                            popular.getOrDefault(nm, 0) + it.optInt("qty")
                    );

                    if ("مكتمل".equals(o.optString("status"))) {
                        String cat = it.optString("category");

                        categoryTotals.put(
                                cat,
                                categoryTotals.getOrDefault(cat, 0)
                                        + it.optInt("total")
                        );
                    }
                }
            }
        }

        StringBuilder report = new StringBuilder();

        report.append("تقرير اليوم ")
                .append(today)
                .append("\n\nالطلبات المكتملة: ")
                .append(todayOrders)
                .append("\nالطلبات الملغاة: ")
                .append(todayCancelled)
                .append("\nعدد المكالمات المسجلة: ")
                .append(callsCount)
                .append("\nإجمالي المبيعات: ")
                .append(money(totalSales))
                .append("\n\nمبيعات الأقسام:\n");

        for (String c : categoryTotals.keySet()) {
            report.append("• ")
                    .append(c)
                    .append(": ")
                    .append(money(categoryTotals.get(c)))
                    .append("\n");
        }

        report.append("\nالأطباق الأكثر طلباً:\n");

        ArrayList<Map.Entry<String, Integer>> popularList =
                new ArrayList<>(popular.entrySet());

        popularList.sort(
                (a, b) -> Integer.compare(b.getValue(), a.getValue())
        );

        for (int i = 0; i < Math.min(10, popularList.size()); i++) {
            report.append(i + 1)
                    .append(". ")
                    .append(popularList.get(i).getKey())
                    .append(" — ")
                    .append(popularList.get(i).getValue())
                    .append(" مرة\n");
        }

        TextView tv = text(report.toString(), 16, NAVY, false);

        tv.setPadding(dp(16), dp(8), dp(16), dp(8));

        ScrollView sc = new ScrollView(this);
        sc.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("تقرير نهاية الدوام")
                .setView(sc)
                .setPositiveButton(
                        "تصدير التقرير",
                        (d, w) -> exportReport()
                )
                .setNeutralButton(
                        "طباعة التقرير",
                        (d, w) -> printHtml(
                                "<html><meta charset='utf-8'><body style='direction:rtl;font-family:sans-serif'>"
                                        + esc(report.toString()).replace("\n", "<br>")
                                        + "</body></html>",
                                "تقرير اليوم"
                        )
                )
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void exportReport() {
        StringBuilder csv = new StringBuilder(
                "رقم الفاتورة,التاريخ,الحالة,النوع,رقم الزبون,السائق,المبلغ\n"
        );

        for (JSONObject o : orders) {
            csv.append(o.optInt("number"))
                    .append(",")
                    .append(csvSafe(o.optString("time")))
                    .append(",")
                    .append(csvSafe(o.optString("status")))
                    .append(",")
                    .append(csvSafe(o.optString("type")))
                    .append(",")
                    .append(csvSafe(o.optString("phone")))
                    .append(",")
                    .append(csvSafe(o.optString("driver")))
                    .append(",")
                    .append(o.optInt("total"))
                    .append("\n");
        }

        Intent send = new Intent(Intent.ACTION_SEND);

        send.setType("text/csv");
        send.putExtra(Intent.EXTRA_SUBJECT, "تقرير مطعم الحجي");
        send.putExtra(Intent.EXTRA_TEXT, csv.toString());

        try {
            startActivity(
                    Intent.createChooser(send, "تصدير تقرير اليوم")
            );
        } catch (Exception e) {
            toast("تعذر فتح المشاركة");
        }
    }

    private String csvSafe(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    private void requestCallScreeningRole() {
        try {
            TelecomManager tm =
                    (TelecomManager) getSystemService(TELECOM_SERVICE);

            if (tm != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !getPackageName().equals(tm.getDefaultDialerPackage())) {

                Intent i = tm.createManageCallScreeningRoleIntent();

                startActivityForResult(i, 44);
            }
        } catch (Exception ignored) {
        }
    }

    private void requestBluetoothPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    },
                    72
            );
        }
    }

    private void loadData() {
        categories.clear();
        dishes.clear();
        drivers.clear();
        orders.clear();

        String raw = prefs.getString(DATA, "");

        if (raw.isEmpty()) {
            categories.add(new Category("الوجبات"));
            categories.add(new Category("المشويات"));
            categories.add(new Category("المقبلات"));
            categories.add(new Category("المشروبات"));

            dishes.add(new Dish("وجبة دجاج", "الوجبات", 5000));
            dishes.add(new Dish("وجبة لحم", "الوجبات", 6000));
            dishes.add(new Dish("كباب", "المشويات", 2500));
            dishes.add(new Dish("تكة", "المشويات", 3000));
            dishes.add(new Dish("حمص", "المقبلات", 1500));
            dishes.add(new Dish("بيبسي", "المشروبات", 500));

            drivers.add("أحمد");
            drivers.add("حسين");

            selectedCategory = "الوجبات";

            return;
        }

        try {
            JSONObject all = new JSONObject(raw);

            JSONArray ca = all.optJSONArray("categories");

            if (ca != null) {
                for (int i = 0; i < ca.length(); i++) {
                    categories.add(new Category(ca.getString(i)));
                }
            }

            JSONArray da = all.optJSONArray("dishes");

            if (da != null) {
                for (int i = 0; i < da.length(); i++) {
                    JSONObject x = da.getJSONObject(i);

                    dishes.add(
                            new Dish(
                                    x.optString("name"),
                                    x.optString("category"),
                                    x.optInt("price")
                            )
                    );
                }
            }

            JSONArray dr = all.optJSONArray("drivers");

            if (dr != null) {
                for (int i = 0; i < dr.length(); i++) {
                    drivers.add(dr.getString(i));
                }
            }

            JSONArray os = all.optJSONArray("orders");

            if (os != null) {
                for (int i = 0; i < os.length(); i++) {
                    orders.add(os.getJSONObject(i));
                }
            }

            nextOrderNo = all.optInt(
                    "nextOrderNo",
                    Math.max(1, orders.size() + 1)
            );

            completedCount = all.optInt("completedCount", 0);
            cancelledCount = all.optInt("cancelledCount", 0);
            callsCount = all.optInt("callsCount", 0);

            selectedCategory = all.optString(
                    "selectedCategory",
                    categories.isEmpty() ? "" : categories.get(0).name
            );

            if (categories.isEmpty()) {
                categories.add(new Category("الوجبات"));
            }

        } catch (Exception e) {
            categories.add(new Category("الوجبات"));
            selectedCategory = "الوجبات";
        }
    }

    private void saveData() {
        try {
            JSONObject all = new JSONObject();

            JSONArray ca = new JSONArray();

            for (Category c : categories) {
                ca.put(c.name);
            }

            all.put("categories", ca);

            JSONArray da = new JSONArray();

            for (Dish d : dishes) {
                JSONObject x = new JSONObject();

                x.put("name", d.name);
                x.put("category", d.category);
                x.put("price", d.price);

                da.put(x);
            }

            all.put("dishes", da);

            JSONArray dr = new JSONArray();

            for (String s : drivers) {
                dr.put(s);
            }

            all.put("drivers", dr);

            JSONArray os = new JSONArray();

            for (JSONObject o : orders) {
                os.put(o);
            }

            all.put("orders", os);

            all.put("nextOrderNo", nextOrderNo);
            all.put("completedCount", completedCount);
            all.put("cancelledCount", cancelledCount);
            all.put("callsCount", callsCount);
            all.put("selectedCategory", selectedCategory);

            prefs.edit()
                    .putString(DATA, all.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("تريد تسجل خروج من الكاشير؟")
                .setPositiveButton(
                        "خروج",
                        (d, w) -> showLogin()
                )
                .setNegativeButton("رجوع", null)
                .show();
    }
}
