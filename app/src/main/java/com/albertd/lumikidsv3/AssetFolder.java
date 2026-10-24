package com.albertd.lumikidsv3;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.*;

public final class AssetFolder {
    private AssetFolder(){}

    public static File copy(Context context, String assetRoot, File dest) throws IOException {
        if (new File(dest,".ready").exists()) return dest;
        if (dest.exists()) delete(dest);
        if (!dest.mkdirs() && !dest.isDirectory()) throw new IOException("Cannot create "+dest);
        copyRecursive(context.getAssets(), assetRoot, dest);
        new File(dest,".ready").createNewFile();
        return dest;
    }

    private static void copyRecursive(AssetManager am, String assetPath, File dest) throws IOException {
        String[] list = am.list(assetPath);
        if (list == null) throw new IOException("Missing asset "+assetPath);
        if (list.length == 0) {
            try(InputStream in=am.open(assetPath); OutputStream out=new FileOutputStream(dest)) {
                byte[] b=new byte[32*1024]; int n;
                while((n=in.read(b))>0) out.write(b,0,n);
            }
            return;
        }
        if(!dest.exists() && !dest.mkdirs()) throw new IOException("Cannot mkdir "+dest);
        for(String child:list) {
            String sub=assetPath+"/"+child;
            String[] subList=am.list(sub);
            File out=new File(dest,child);
            if(subList!=null && subList.length>0) copyRecursive(am,sub,out);
            else {
                try(InputStream in=am.open(sub); OutputStream os=new FileOutputStream(out)) {
                    byte[] b=new byte[32*1024]; int n;
                    while((n=in.read(b))>0) os.write(b,0,n);
                }
            }
        }
    }

    public static void delete(File f) {
        if(f.isDirectory()) {
            File[] xs=f.listFiles();
            if(xs!=null) for(File x:xs) delete(x);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
