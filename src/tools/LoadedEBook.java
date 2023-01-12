package tools;

import java.io.IOException;

/**
 * 拷贝电子书的工具（自动化截屏脚本）
 * */
public class LoadedEBook {

    /** 删除文件指令 */
    public static final String CMD_REMOVE_FILE = "rm -rf %s";

    /** 左滑翻页指令 */
    public static final String CMD_SWIPE = "adb shell input swipe 900 900 800 900 10";

    /** 音量减键指令，用于翻页 */
    public static final String CMD_NEXTPAGE = "adb shell input keyevent 25";

    /** 截屏并保存文件指令 */
    public static final String CMD_CAPTURE = "adb exec-out screencap -p > %s";

    /** 将截屏转为png8，减少图片提及，需安装Imagemagick */
    public static final String CMD_CONVERT = "convert %s PNG8:%s";

    /** 计算md5指令 */
    public static final String CMD_MD5 = "md5 %s";

    /**
     * 紧跟两个int类型参数，第一个表示总页数，必传，第二个表示起始页，可不传，默认为1。
     * */
    public static void main(String[] args) throws InterruptedException, IOException {
        int count = Integer.parseInt(args[0]);
        int offset = 1;
        if(args.length > 1){
            offset = Integer.parseInt(args[1]);
        }

        String fileName;  // 当前处理的截图文件文件名
        String convertFileName;  // 转换后的文件名
        String oldFileName = null;  // 缓存上一个截图，用于对比

        for (int i = offset; i <= count; i++) {

            System.out.println(String.format("page %d is start", i));

            fileName = i + ".png";
            convertFileName = i + "n.png";

            // 截图
            command[2] = String.format(CMD_CAPTURE, fileName);
            runCmd(command);

            // 判断两页是否重复
            if(oldFileName != null){
                command[2] = String.format(CMD_MD5, oldFileName);
                String oldMd5 = parserMd5(runCmd(command));

                command[2] = String.format(CMD_MD5, fileName);
                String newMd5 = parserMd5(runCmd(command));

//                System.out.println(String.format("oldMd5 = %s, newMd5 = %s, equals = %b",oldMd5, newMd5, oldMd5.equals(newMd5)));

                // 两个截图md5相同，说明翻页不及时，需要重新截图
                if(oldMd5.equals(newMd5)){
                    System.out.println(String.format("------------------page %d is error", i));
                    rmFile(fileName);  // 删除重复文件
                    i--;
                    continue;
                }
            }

            // 翻页
            command[2] = CMD_NEXTPAGE;
            runCmd(command);

            // 压缩图片
            command[2] = String.format(CMD_CONVERT, fileName, convertFileName);
            runCmd(command);

            // 删除旧页
            if(oldFileName != null){
                rmFile(oldFileName);
            }

            // 缓存当前页，供下次使用
            oldFileName = fileName;

            System.out.println(String.format("page %d is success", i));
        }

        // 截图完毕后，删除缓存的旧截图文件
        if(oldFileName != null){
            rmFile(oldFileName);
        }
    }

    /** 删除给定名称的文件 */
    private static void rmFile(String fileName) throws IOException, InterruptedException {
        command[2] = String.format(CMD_REMOVE_FILE, fileName);
        runCmd(command);
    }

    private static byte[] cache = new byte[4096];
    private static String[] command = {"sh", "-c", ""};

    /** 执行命令 */
    private static String runCmd(String[] command) throws IOException, InterruptedException {
        Process exec = Runtime.getRuntime().exec(command);
        exec.waitFor();
        return readFromCmd(exec);
    }

    /** 从Process中读取输出的信息 */
    private static String readFromCmd(Process process) throws IOException {
        int bit = process.getInputStream().read(cache);
        if(bit == -1){
            return "";
        }
        String x = new String(cache, 0, bit);
//        System.out.println(x);
        return x;
    }

    /** 解析md5指令的返回结果，返回md5字符串 */
    private static String parserMd5(String processResult){
        String[] s = processResult.split(" ");
        return s[s.length - 1];
    }
}

