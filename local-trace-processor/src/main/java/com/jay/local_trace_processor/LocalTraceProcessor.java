package com.jay.local_trace_processor;

import static com.bytedance.rheatrace.processor.Main.usage;

import com.bytedance.rheatrace.processor.Main;
import com.bytedance.rheatrace.processor.core.Arguments;
import com.bytedance.rheatrace.processor.core.Debug;
import com.bytedance.rheatrace.processor.core.SystemLevelCapture;
import com.bytedance.rheatrace.processor.core.Version;
import com.bytedance.rheatrace.processor.core.Workspace;
import com.bytedance.rheatrace.processor.lite.LiteCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class LocalTraceProcessor {

    private static String sSourceDir = null;


    public static void main(String[] args) throws Exception {
        if (args.length == 0 || Arrays.asList(args).contains("-v")) {
            System.out.println("Version: " + Version.NAME);
            System.out.println("  Usage: " + usage());
            return;
        }

        if (args[0].equals("local")) {
            Debug.init(args);
            Arguments.init(args);
            resolveArgs(args);

            File workspace = Workspace.appAtrace().getParentFile();
            String sourceDir = sSourceDir;
            copyFilesOneLevel(new File(sourceDir), workspace);

            SystemLevelCapture sysCapture = new LiteCapture();
            sysCapture.process();
        } else {
            Main.main(args);
        }
    }

    public static String[] appendArgs(String[] args) {
        // 计算新数组长度（原数组长度 + 2）
        int newLength = args.length + 2;
        String[] newArgs = new String[newLength];

        // 复制原数组内容
        System.arraycopy(args, 0, newArgs, 0, args.length);

        // 添加新的元素
        newArgs[args.length] = "-a";
        newArgs[args.length + 1] = "com.test.default";

        return newArgs;
    }

    public static void copyFilesOneLevel(File source, File target) {
        // 检查源目录是否存在
        if (!source.exists() || !source.isDirectory()) {
            System.err.println("Source directory does not exist: " + source.getAbsolutePath());
            return;
        }

        // 确保目标目录存在
        if (!target.exists() && !target.mkdirs()) {
            System.err.println("Failed to create target directory: " + target.getAbsolutePath());
            return;
        }

        // 获取源目录下的所有文件（仅文件，忽略子目录）
        File[] files = source.listFiles();
        if (files == null) {
            System.err.println("Failed to list files in: " + source.getAbsolutePath());
            return;
        }

        for (File file : files) {
            if (file.isFile()) {  // 只处理文件，忽略子目录
                copyFile(file, new File(target, file.getName()));
            }
        }
    }

    private static void copyFile(File sourceFile, File destFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.err.println("Failed to copy file: " + sourceFile.getAbsolutePath());
            e.printStackTrace();
        } finally {
            // 确保流关闭，防止资源泄露
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void resolveArgs(String[] args) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            switch (arg) {
                case "-source":
                    sSourceDir = args[i++];
            }
        }
    }
}