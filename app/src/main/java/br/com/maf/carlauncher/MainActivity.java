
package br.com.maf.carlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(7, 12, 19);
    private static final int CYAN = Color.rgb(38, 176, 255);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(177, 192, 207);
    private static final int PICK_WALLPAPER = 741;

    private SharedPreferences prefs;
    private FrameLayout root;
    private ImageView wallpaper;
    private LinearLayout dock;
    private Slot left, right;
    private String lastPackage;

    private class Slot {
        String key, pkg;
        LinearLayout panel;
        ImageView icon;
        TextView title, subtitle;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences("tiggo8_launcher", MODE_PRIVATE);
        lastPackage = prefs.getString("last_package", null);

        immersive();
        buildUi();
        restore(left);
        restore(right);
        applyWallpaper();
        renderDock();
    }

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void immersive() {
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));

        if (stroke != 0) {
            drawable.setStroke(dp(1), stroke);
        }

        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(
            wallpaper,
            new FrameLayout.LayoutParams(-1, -1)
        );

        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(125, 0, 4, 10));
        root.addView(
            shade,
            new FrameLayout.LayoutParams(-1, -1)
        );

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setPadding(dp(12), dp(10), dp(12), dp(10));

        root.addView(
            main,
            new FrameLayout.LayoutParams(-1, -1)
        );

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.VERTICAL);
        dock.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        dock.setPadding(dp(5), dp(7), dp(5), dp(7));
        dock.setBackground(shape(
            Color.argb(165, 20, 32, 45),
            24,
            Color.argb(175, 117, 164, 199)
        ));

        LinearLayout.LayoutParams dockParams =
            new LinearLayout.LayoutParams(dp(72), -1);
        dockParams.setMargins(0, 0, dp(10), 0);
        main.addView(dock, dockParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        main.addView(body, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = text("TIGGO 8 PRO", 18, WHITE, true);
        brand.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

        header.addView(
            brand,
            new LinearLayout.LayoutParams(0, dp(38), 1)
        );

        TextView gear = text("⚙", 27, WHITE, false);
        gear.setBackground(shape(
            Color.argb(165, 8, 15, 23),
            20,
            Color.argb(120, 130, 170, 202)
        ));

        header.addView(gear, new LinearLayout.LayoutParams(dp(46), dp(40)));
        gear.setOnClickListener(v -> settings());
        body.addView(header);

        LinearLayout panes = new LinearLayout(this);
        panes.setOrientation(LinearLayout.HORIZONTAL);

        left = slot(
            "left",
            "NAVEGAÇÃO",
            "Escolha Google Maps, Waze ou outro app"
        );

        right = slot(
            "right",
            "MÍDIA",
            "Escolha música ou outro aplicativo"
        );

        panes.addView(
            left.panel,
            new LinearLayout.LayoutParams(0, -1, 1)
        );

        panes.addView(new View(this), new LinearLayout.LayoutParams(dp(9), 1));

        panes.addView(
            right.panel,
            new LinearLayout.LayoutParams(0, -1, 1)
        );

        body.addView(panes, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = text(
            "Toque em um atalho para abrir o aplicativo em tela cheia",
            10,
            MUTED,
            false
        );

        footer.setPadding(0, dp(6), 0, 0);
        body.addView(footer);

        setContentView(root);
    }

    private Slot slot(String key, String heading, String description) {
        Slot slot = new Slot();
        slot.key = key;

        slot.panel = new LinearLayout(this);
        slot.panel.setOrientation(LinearLayout.VERTICAL);
        slot.panel.setGravity(Gravity.CENTER);
        slot.panel.setPadding(dp(14), dp(12), dp(14), dp(12));

        slot.panel.setBackground(shape(
            Color.argb(205, 9, 16, 25),
            25,
            Color.argb(180, 84, 147, 190)
        ));

        TextView headingView = text(heading, 11, CYAN, true);
        headingView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

        slot.panel.addView(
            headingView,
            new LinearLayout.LayoutParams(-1, dp(27))
        );

        slot.icon = new ImageView(this);
        slot.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams iconParams =
            new LinearLayout.LayoutParams(dp(76), dp(76));

        iconParams.setMargins(0, dp(12), 0, dp(12));
        slot.panel.addView(slot.icon, iconParams);

        slot.title = text("Selecionar aplicativo", 18, WHITE, true);
        slot.panel.addView(
            slot.title,
            new LinearLayout.LayoutParams(-1, dp(42))
        );

        slot.subtitle = text(description, 12, MUTED, false);
        slot.panel.addView(
            slot.subtitle,
            new LinearLayout.LayoutParams(-1, dp(44))
        );

        TextView choose = text("＋  Escolher aplicativo", 14, WHITE, true);
        choose.setBackground(shape(Color.argb(220, 23, 99, 163), 21, 0));

        LinearLayout.LayoutParams chooseParams =
            new LinearLayout.LayoutParams(-1, dp(47));

        chooseParams.setMargins(0, dp(10), 0, dp(7));
        slot.panel.addView(choose, chooseParams);
        choose.setOnClickListener(v -> chooseApp(slot));

        TextView open = text("Abrir em tela cheia  ↗", 13, WHITE, false);
        open.setBackground(shape(
            Color.argb(135, 20, 29, 40),
            19,
            Color.argb(130, 115, 155, 185)
        ));

        slot.panel.addView(
            open,
            new LinearLayout.LayoutParams(-1, dp(42))
        );

        // Botão explícito para solicitar a abertura adjacente.
        open.setOnClickListener(v -> launchAdjacent(slot.pkg));

        // Tocar no painel também solicita abertura adjacente.
        slot.panel.setOnClickListener(v -> {
            if (slot.pkg == null) {
                chooseApp(slot);
            } else {
                launchAdjacent(slot.pkg);
            }
        });

        return slot;
    }

    private void restore(Slot slot) {
        String pkg = prefs.getString(slot.key + "_package", null);

        if (pkg == null) {
            return;
        }

        try {
            ApplicationInfo info =
                getPackageManager().getApplicationInfo(pkg, 0);

            slot.pkg = pkg;
            slot.title.setText(
                getPackageManager().getApplicationLabel(info)
            );
            slot.subtitle.setText("Toque para abrir o aplicativo");
            slot.icon.setImageDrawable(
                getPackageManager().getApplicationIcon(info)
            );

        } catch (Exception e) {
            prefs.edit().remove(slot.key + "_package").apply();
        }
    }

    private List<ApplicationInfo> apps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> applications = new ArrayList<>();

        for (ApplicationInfo info :
            pm.getInstalledApplications(PackageManager.GET_META_DATA)) {

            if (pm.getLaunchIntentForPackage(info.packageName) != null
                && !info.packageName.equals(getPackageName())) {
                applications.add(info);
            }
        }

        Collections.sort(applications, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString()
                    .compareToIgnoreCase(
                        pm.getApplicationLabel(b).toString()
                    );
            }
        });

        return applications;
    }

    private void chooseApp(Slot slot) {
        List<ApplicationInfo> applications = apps();
        String[] names = new String[applications.size()];
        PackageManager pm = getPackageManager();

        for (int i = 0; i < applications.size(); i++) {
            names[i] = pm.getApplicationLabel(
                applications.get(i)
            ).toString();
        }

        if (applications.isEmpty()) {
            Toast.makeText(
                this,
                "Nenhum aplicativo compatível encontrado.",
                Toast.LENGTH_LONG
            ).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Escolher aplicativo")
            .setItems(names, (dialog, position) -> {
                ApplicationInfo info = applications.get(position);

                slot.pkg = info.packageName;
                slot.title.setText(names[position]);
                slot.subtitle.setText("Toque para abrir o aplicativo");

                try {
                    slot.icon.setImageDrawable(
                        pm.getApplicationIcon(info)
                    );
                } catch (Exception ignored) {
                }

                prefs.edit()
                    .putString(slot.key + "_package", slot.pkg)
                    .apply();

                renderDock();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // Solicita ao Android que abra o app junto a outra tarefa.
    // O firmware precisa oferecer suporte a tela dividida.
    private void launchAdjacent(String pkg) {
        if (pkg == null) {
            new AlertDialog.Builder(this)
                .setMessage("Escolha um aplicativo primeiro.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        try {
            Intent intent =
                getPackageManager().getLaunchIntentForPackage(pkg);

            if (intent == null) {
                Toast.makeText(
                    this,
                    "Não foi possível abrir este aplicativo.",
                    Toast.LENGTH_LONG
                ).show();
                return;
            }

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
            );

            lastPackage = pkg;
            prefs.edit().putString("last_package", pkg).apply();

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                this,
                "A central pode não permitir tela dividida.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    // Abertura normal em tela cheia para os atalhos laterais.
    private void launch(String pkg) {
        if (pkg == null) {
            new AlertDialog.Builder(this)
                .setMessage("Escolha um aplicativo primeiro.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        try {
            Intent intent =
                getPackageManager().getLaunchIntentForPackage(pkg);

            if (intent == null) {
                return;
            }

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
            );

            lastPackage = pkg;
            prefs.edit().putString("last_package", pkg).apply();
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                this,
                "A central não permitiu abrir o aplicativo.",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    private void renderDock() {
        if (dock == null) {
            return;
        }

        dock.removeAllViews();

        dock.addView(
            text("APP", 10, WHITE, true),
            new LinearLayout.LayoutParams(-1, dp(28))
        );

        List<String> pins = new ArrayList<>();

        add(pins, prefs.getString("left_package", null));
        add(pins, prefs.getString("right_package", null));
        add(pins, lastPackage);

        String extra = prefs.getString("pinned_packages", "");

        if (!extra.isEmpty()) {
            for (String pkg : extra.split("\\|")) {
                add(pins, pkg);
            }
        }

        for (String pkg : pins) {
            addIcon(pkg);
        }

        View spacer = new View(this);
        dock.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        TextView all = text("⠿", 28, WHITE, false);
        all.setBackground(shape(Color.argb(100, 80, 100, 120), 16, 0));

        dock.addView(all, new LinearLayout.LayoutParams(-1, dp(48)));
        all.setOnClickListener(v -> appManager());
    }

    private void add(List<String> list, String pkg) {
        if (pkg != null
            && !pkg.isEmpty()
            && !pkg.equals(getPackageName())
            && !list.contains(pkg)) {
            list.add(pkg);
        }
    }

    private void addIcon(String pkg) {
        try {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(
                getPackageManager().getApplicationIcon(pkg)
            );
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setPadding(dp(7), dp(7), dp(7), dp(7));
            icon.setBackground(shape(
                Color.argb(190, 239, 247, 255),
                16,
                Color.argb(130, 180, 215, 240)
            ));

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(46));

            params.setMargins(0, dp(5), 0, dp(5));
            dock.addView(icon, params);

            icon.setOnClickListener(v -> launch(pkg));
            icon.setOnLongClickListener(v -> {
                appManager();
                return true;
            });

        } catch (Exception ignored) {
        }
    }

    private void settings() {
        String[] options = {
            "Aparência e wallpaper",
            "Aplicativos do menu lateral",
            "Abrir último aplicativo",
            "Sobre"
        };

        new AlertDialog.Builder(this)
            .setTitle("Configurações • Tiggo 8 Pro")
            .setItems(options, (dialog, position) -> {
                if (position == 0) {
                    wallpaperSettings();
                } else if (position == 1) {
                    appManager();
                } else if (position == 2) {
                    launch(lastPackage);
                } else {
                    new AlertDialog.Builder(this)
                        .setTitle("Tiggo 8 Pro Launcher")
                        .setMessage(
                            "Launcher experimental para Android 10. "
                            + "A abertura lado a lado depende do suporte "
                            + "a múltiplas janelas do sistema."
                        )
                        .setPositiveButton("OK", null)
                        .show();
                }
            })
            .show();
    }

    private void wallpaperSettings() {
        String[] options = {
            "Escolher imagem da galeria",
            "Remover wallpaper personalizado"
        };

        new AlertDialog.Builder(this)
            .setTitle("Aparência e wallpaper")
            .setItems(options, (dialog, position) -> {
                if (position == 0) {
                    Intent intent =
                        new Intent(Intent.ACTION_OPEN_DOCUMENT);

                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    );

                    startActivityForResult(intent, PICK_WALLPAPER);

                } else {
                    prefs.edit().remove("wallpaper_uri").apply();
                    wallpaper.setImageDrawable(null);

                    Toast.makeText(
                        this,
                        "Wallpaper padrão restaurado.",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            })
            .show();
    }

    private void applyWallpaper() {
        String uri = prefs.getString("wallpaper_uri", null);

        if (uri != null) {
            try {
                wallpaper.setImageURI(Uri.parse(uri));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_WALLPAPER
            && resultCode == RESULT_OK
            && data != null
            && data.getData() != null) {

            Uri uri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                wallpaper.setImageURI(uri);
                prefs.edit()
                    .putString("wallpaper_uri", uri.toString())
                    .apply();

                Toast.makeText(
                    this,
                    "Wallpaper aplicado à tela principal.",
                    Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "Não foi possível aplicar a imagem escolhida.",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void appManager() {
        List<ApplicationInfo> applications = apps();
        PackageManager pm = getPackageManager();
        String[] names = new String[applications.size()];

        for (int i = 0; i < applications.size(); i++) {
            names[i] = pm.getApplicationLabel(
                applications.get(i)
            ).toString();
        }

        if (applications.isEmpty()) {
            Toast.makeText(
                this,
                "Nenhum aplicativo compatível encontrado.",
                Toast.LENGTH_LONG
            ).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Adicionar aplicativo ao menu lateral")
            .setItems(names, (dialog, position) -> {
                List<String> pins = new ArrayList<>();
                String saved = prefs.getString("pinned_packages", "");

                if (!saved.isEmpty()) {
                    for (String pkg : saved.split("\\|")) {
                        add(pins, pkg);
                    }
                }

                add(pins, applications.get(position).packageName);

                StringBuilder builder = new StringBuilder();

                for (String pkg : pins) {
                    if (builder.length() > 0) {
                        builder.append('|');
                    }
                    builder.append(pkg);
                }

                prefs.edit()
                    .putString("pinned_packages", builder.toString())
                    .apply();

                renderDock();

                Toast.makeText(
                    this,
                    names[position] + " adicionado ao menu lateral.",
                    Toast.LENGTH_SHORT
                ).show();
            })
            .setNegativeButton("Fechar", null)
            .show();
    }
}
