package com.vanipombe.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private TextView status, stats;
    private LinearLayout results;
    private DocumentFile source, destination;
    private ExecutorService executor;
    private volatile boolean stopped;
    private int found, copied, failed;
    private final Handler main = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> picker =
        registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {}
            DocumentFile d = DocumentFile.fromTreeUri(this, uri);
            if (d == null) return;
            if (source == null) { source = d; status.setText("Source: " + d.getName()); }
            else { destination = d; status.setText("Destination: " + d.getName()); }
        });

    @Override public void onCreate(Bundle b) { super.onCreate(b); buildUi(); }

    private void buildUi() {
        ScrollView s = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(28,28,28,28);
        TextView title = new TextView(this); title.setText("VANIPOMBE"); title.setTextSize(30); title.setGravity(Gravity.CENTER);
        box.addView(title);
        TextView sub = new TextView(this); sub.setText("TEVETA 2026 File Search & Copy"); sub.setGravity(Gravity.CENTER); box.addView(sub);

        Button src = new Button(this); src.setText("Choose Source Folder");
        src.setOnClickListener(v -> { source = null; picker.launch(null); }); box.addView(src);
        Button dst = new Button(this); dst.setText("Choose Destination Folder");
        dst.setOnClickListener(v -> picker.launch(null)); box.addView(dst);
        Button run = new Button(this); run.setText("Search & Copy");
        run.setOnClickListener(v -> startSearch()); box.addView(run);
        Button stop = new Button(this); stop.setText("Stop");
        stop.setOnClickListener(v -> stopped = true); box.addView(stop);

        status = new TextView(this); status.setText("Choose source and destination folders."); status.setPadding(0,20,0,10); box.addView(status);
        stats = new TextView(this); stats.setText("Found: 0   Copied: 0   Failed: 0"); box.addView(stats);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); box.addView(results);
        s.addView(box); setContentView(s);
    }

    private void startSearch() {
        if (source == null || destination == null) { Toast.makeText(this,"Choose both folders first.",Toast.LENGTH_LONG).show(); return; }
        stopped=false; found=copied=failed=0; results.removeAllViews(); status.setText("Searching...");
        executor=Executors.newFixedThreadPool(Math.max(2,Runtime.getRuntime().availableProcessors()));
        executor.submit(() -> scan(source));
    }

    private void scan(DocumentFile dir) {
        if (stopped) return;
        for (DocumentFile f: dir.listFiles()) {
            if (stopped) break;
            if (f.isDirectory()) scan(f);
            else if (matches(f.getName())) {
                found++;
                main.post(() -> { stats.setText("Found: "+found+"   Copied: "+copied+"   Failed: "+failed); TextView t=new TextView(this); t.setText("FOUND: "+f.getName()); results.addView(t); });
                if (copy(f)) copied++; else failed++;
                main.post(() -> stats.setText("Found: "+found+"   Copied: "+copied+"   Failed: "+failed));
            }
        }
        if (dir==source) main.post(() -> status.setText(stopped?"Stopped.":"Finished."));
    }

    private boolean matches(String name) {
        if (name==null) return false;
        String n=name.toUpperCase(Locale.ROOT);
        boolean ext=n.matches(".*\\.(DOC|DOCX|PDF|XLS|XLSX|PPT|PPTX|TXT|JPG|JPEG|PNG)$");
        if (!ext) return false;
        return n.contains("TEVETA") || n.contains("2026") || n.contains("DRAFT") || n.matches(".*(^|[^A-Z])D[1-4]([^A-Z]|$).*");
    }

    private boolean copy(DocumentFile src) {
        try {
            String base=src.getName()==null?"file":src.getName(), name=base, stem=base, ext="";
            int dot=base.lastIndexOf('.');
            if(dot>0){stem=base.substring(0,dot);ext=base.substring(dot);}
            int i=1; while(destination.findFile(name)!=null) name=stem+"_"+(i++)+ext;
            DocumentFile out=destination.createFile("application/octet-stream",name);
            if(out==null)return false;
            try(java.io.InputStream in=getContentResolver().openInputStream(src.getUri());
                java.io.OutputStream os=getContentResolver().openOutputStream(out.getUri())){
                if(in==null||os==null)return false;
                byte[] b=new byte[1024*1024]; int n;
                while((n=in.read(b))!=-1){ if(stopped)return false; os.write(b,0,n); }
            }
            return true;
        }catch(Exception e){return false;}
    }

    @Override protected void onDestroy(){stopped=true;if(executor!=null)executor.shutdownNow();super.onDestroy();}
}
