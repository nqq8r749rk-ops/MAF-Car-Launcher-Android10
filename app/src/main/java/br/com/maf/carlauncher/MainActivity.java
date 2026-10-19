package br.com.maf.carlauncher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(5,10,17), PANEL = Color.rgb(15,25,37);
    private final int CYAN = Color.rgb(47,181,255), WHITE = Color.rgb(245,249,255), MUTED = Color.rgb(155,174,194);
    private SharedPreferences prefs;
    private Slot left, right;
    private Button pairButton;

    static class Slot {
        String label = "Nenhum aplicativo selecionado", packageName;
        LinearLayout container;
        TextView title;
        Button choose, open;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("maf_launcher", MODE_PRIVATE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        buildUi();
        restore(left, "left"); restore(right, "right"); refreshPairButton();
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(2, stroke); return d;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String s, boolean primary) {
        Button b = new Button(this); b.setText(s); b.setTextSize(12); b.setAllCaps(false);
        b.setTextColor(WHITE); b.setBackground(shape(primary ? Color.rgb(24,126,205) : Color.rgb(27,42,58), 24, primary ? Color.rgb(67,190,255) : Color.rgb(61,91,119))); return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,10,20,12); root.setBackgroundColor(BG);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("TIGGO 8 PRO",20,WHITE,true);
        brand.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        header.addView(brand,new LinearLayout.LayoutParams(0,42,1));
        header.addView(text("SMART DRIVE  •  MULTIMÍDIA",10,CYAN,true)); root.addView(header);

        LinearLayout panes = new LinearLayout(this); panes.setOrientation(LinearLayout.HORIZONTAL);
        panes.setPadding(0,10,0,12);
        left = makeSlot("PAINEL ESQUERDO","left"); right = makeSlot("PAINEL DIREITO","right");
        panes.addView(left.container,new LinearLayout.LayoutParams(0,-1,1));
        View divider = new View(this); divider.setBackgroundColor(Color.rgb(47,181,255));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(2,-1); dp.setMargins(12,12,12,12); panes.addView(divider,dp);
        panes.addView(right.container,new LinearLayout.LayoutParams(0,-1,1));
        root.addView(panes,new LinearLayout.LayoutParams(-1,0,1));
        pairButton = button("ABRIR OS DOIS APLICATIVOS",true);
        root.addView(pairButton,new LinearLayout.LayoutParams(-1,46));
        pairButton.setOnClickListener(v -> launchPair());
        TextView footer = text("NAVEGAÇÃO  •  MÍDIA  •  ACESSO RÁPIDO",10,MUTED,true);
        footer.setPadding(0,4,0,0); root.addView(footer);
        setContentView(root);
    }

    private Slot makeSlot(String heading, String key) {
        Slot s = new Slot(); s.container = new LinearLayout(this);
        s.container.setOrientation(LinearLayout.VERTICAL); s.container.setGravity(Gravity.CENTER);
        s.container.setPadding(16,12,16,12); s.container.setBackground(shape(PANEL,26,Color.rgb(48,103,145)));
        s.container.addView(text(heading,12,CYAN,true),new LinearLayout.LayoutParams(-1,32));
        View line = new View(this); line.setBackgroundColor(Color.rgb(43,72,99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,1); lp.setMargins(8,1,8,8); s.container.addView(line,lp);
        s.container.addView(text("◈",38,CYAN,false),new LinearLayout.LayoutParams(-1,52));
        s.title = text(s.label,17,WHITE,true); s.container.addView(s.title,new LinearLayout.LayoutParams(-1,0,1));
        s.choose = button("＋  Escolher aplicativo",true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,44); cp.setMargins(2,6,2,5); s.container.addView(s.choose,cp);
        s.open = button("Abrir aplicativo  ↗",false);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1,42); op.setMargins(2,2,2,2); s.container.addView(s.open,op);
        s.choose.setOnClickListener(v -> chooseApp(s,key)); s.open.setOnClickListener(v -> launchOne(s));
        return s;
    }

    private void restore(Slot slot, String key) {
        String pkg = prefs.getString(key+"_package",null); if (pkg == null) return;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg,0);
            slot.packageName = pkg; slot.label = getPackageManager().getApplicationLabel(ai).toString();
            slot.title.setText(slot.label); slot.open.setText("Abrir " + slot.label);
        } catch (Exception e) { prefs.edit().remove(key+"_package").apply(); }
    }

    private void chooseApp(Slot slot, String key) {
        PackageManager pm = getPackageManager(); final List<ApplicationInfo> apps = new ArrayList<>();
        for (ApplicationInfo ai : pm.getInstalledApplications(PackageManager.GET_META_DATA))
            if (pm.getLaunchIntentForPackage(ai.packageName) != null && !ai.packageName.equals(getPackageName())) apps.add(ai);
        Collections.sort(apps,new Comparator<ApplicationInfo>() {
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });
        List<String> names = new ArrayList<>();
        for (ApplicationInfo ai : apps) names.add(pm.getApplicationLabel(ai).toString());
        if (names.isEmpty()) { Toast.makeText(this,"Nenhum aplicativo compatível encontrado.",Toast.LENGTH_LONG).show(); return; }
        new AlertDialog.Builder(this).setTitle("Escolher aplicativo").setItems(names.toArray(new String[0]),(d,which)->{
            ApplicationInfo ai = apps.get(which); slot.packageName = ai.packageName;
            slot.label = pm.getApplicationLabel(ai).toString(); slot.title.setText(slot.label);
            slot.open.setText("Abrir " + slot.label);
            prefs.edit().putString(key+"_package",slot.packageName).apply(); refreshPairButton();
        }).setNegativeButton("Cancelar",null).show();
    }

    private void refreshPairButton() {
        if (pairButton == null) return;
        boolean ready = left.packageName != null && right.packageName != null;
        pairButton.setText(ready ? "Tentar abrir os dois lado a lado" : "Selecione um aplicativo em cada painel");
        pairButton.setEnabled(ready); pairButton.setAlpha(ready ? 1f : .65f);
    }

    private void launchOne(Slot slot) {
        if (slot.packageName == null) { Toast.makeText(this,"Escolha um aplicativo primeiro.",Toast.LENGTH_SHORT).show(); return; }
        Intent i = getPackageManager().getLaunchIntentForPackage(slot.packageName);
        if (i == null) return;
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT); startActivity(i); }
        catch (Exception e) { Toast.makeText(this,"A central não permitiu abrir o aplicativo.",Toast.LENGTH_LONG).show(); }
    }

    private void launchPair() {
        if (left.packageName == null || right.packageName == null) return;
        if (left.packageName.equals(right.packageName)) {
            Toast.makeText(this,"Escolha dois aplicativos diferentes.",Toast.LENGTH_LONG).show(); return;
        }
        Intent a = getPackageManager().getLaunchIntentForPackage(left.packageName);
        Intent b = getPackageManager().getLaunchIntentForPackage(right.packageName);
        if (a == null || b == null) return;
        try {
            int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
            a.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            ActivityOptions oa = ActivityOptions.makeBasic(); oa.setLaunchBounds(new Rect(0,0,w/2,h)); startActivity(a,oa.toBundle());
            b.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
            ActivityOptions ob = ActivityOptions.makeBasic(); ob.setLaunchBounds(new Rect(w/2,0,w,h)); startActivity(b,ob.toBundle());
            Toast.makeText(this,"Solicitação enviada. A divisão depende do sistema da central.",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,"Tela dividida não aceita pela central. Teste a divisão manual no menu de apps recentes.",Toast.LENGTH_LONG).show();
        }
    }
}
