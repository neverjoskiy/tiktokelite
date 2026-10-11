package com.neverocheg.elite;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.Adler32;

/** Static inspection only. Never loads a plugin or claims to sandbox its code. */
public final class PluginAudit {
    private PluginAudit() {}
    public static final int MAX_DEX=16*1024*1024;
    public static final class Report {
        public final Set<String> findings=new LinkedHashSet<String>();
        public boolean blocked;
        public String digest="";
        public String pluginId="";
        private void add(String text,boolean deny){findings.add(text);blocked|=deny;}
        public String summary(){
            StringBuilder text=new StringBuilder(blocked?"Установка заблокирована":"Статическая проверка завершена");
            for(String item:findings)text.append("\n• ").append(item);
            if(findings.isEmpty())text.append("\nИзвестные опасные API не обнаружены.");
            return text.append("\n\nПроверка не гарантирует безопасность: плагин работает с правами TikTok. После установки он выключен.").toString();
        }
    }
    public static byte[] read(File file,int limit)throws IOException{
        try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;
            while((n=in.read(b))!=-1){total+=n;if(total>limit)throw new IOException("Превышен размер файла плагина");out.write(b,0,n);}return out.toByteArray();}
    }
    /** Длинная base64-строка допустима, только если это настоящая картинка (бейджи, иконки). */
    static boolean embeddedImage(String text){
        try{
            byte[] b=java.util.Base64.getMimeDecoder().decode(text.replace('-','+').replace('_','/'));
            if(b.length>2*1024*1024||b.length<12)return false;
            int n=b.length;
            if((b[0]&255)==0xFF&&(b[1]&255)==0xD8&&(b[2]&255)==0xFF)return (b[n-2]&255)==0xFF&&(b[n-1]&255)==0xD9;
            if((b[0]&255)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G')return new String(b,n-16,16,"ISO-8859-1").contains("IEND");
            return b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P';
        }catch(Exception e){return false;}
    }
    public static String sha256(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString();}
    public static Report folder(File folder)throws Exception{
        byte[] dex=read(new File(folder,"classes.dex"),MAX_DEX);
        Report report=dex(dex);
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        for(String name:new String[]{"manifest.json","classes.dex","icon.png"}){
            hash.update(name.getBytes("UTF-8"));File file=new File(folder,name);
            if(file.isFile())hash.update(MessageDigest.getInstance("SHA-256").digest(read(file,name.equals("classes.dex")?MAX_DEX:name.equals("icon.png")?1024*1024:65536)));
        }
        report.digest=hex(hash.digest());return report;
    }
    public static Report dex(byte[] data)throws Exception{
        Report report=new Report();Dex d=new Dex(data);
        for(int i=0;i<d.typeCount;i++){
            String type=d.type(i);
            if(type.startsWith("Ldalvik/system/") && (type.contains("ClassLoader")||type.contains("DexFile")))report.add("Загрузка дополнительного исполняемого кода",true);
            if(type.startsWith("Ljava/lang/reflect/")||type.startsWith("Ljava/lang/invoke/"))report.add("Рефлексия: часть действий нельзя установить статически",false);
            if(type.startsWith("Ljava/net/")||type.startsWith("Lokhttp")||type.startsWith("Lretrofit"))report.add("Доступ к сети",false);
            if(type.equals("Landroid/webkit/WebView;"))report.add("WebView и возможное выполнение JavaScript",false);
            if(type.startsWith("Landroid/telephony/")||type.startsWith("Landroid/provider/Contacts")||type.startsWith("Landroid/accounts/"))report.add("Доступ к личным данным устройства",false);
            if(type.equals("Landroid/content/ClipboardManager;"))report.add("Доступ к буферу обмена",false);
            if(type.startsWith("Ljava/io/")||type.startsWith("Ljava/nio/file/"))report.add("Прямой доступ к файлам приложения",false);
        }
        for(int i=0;i<d.methodCount;i++){
            int p=d.methodOff+i*8;String owner=d.type(d.u16(p));String name=d.string(d.u32(p+4));
            if((owner.equals("Ljava/lang/Runtime;")&&name.equals("exec"))||(owner.equals("Ljava/lang/ProcessBuilder;")&&name.equals("start")))report.add("Запуск системных команд",true);
            if((owner.equals("Ljava/lang/System;")||owner.equals("Ljava/lang/Runtime;"))&&(name.equals("load")||name.equals("loadLibrary")))report.add("Загрузка нативных библиотек",true);
            if(owner.equals("Ljava/lang/Class;")&&name.equals("forName"))report.add("Поиск классов по имени: возможна скрытая загрузка API",false);
            if((owner.equals("Lcom/neverocheg/elite/plugin/PluginContext;")&&(name.equals("fetch")||name.equals("download")))||owner.equals("Lcom/neverocheg/elite/Net;"))report.add("Сеть через API мода",false);
            if((owner.equals("Lcom/neverocheg/elite/plugin/PluginContext;")&&name.equals("context"))||(owner.equals("Lcom/neverocheg/elite/Margy;")&&name.equals("context")))report.add("Доступ к контексту TikTok",false);
            if(owner.equals("Ljava/lang/ClassLoader;")&&name.equals("loadClass"))report.add("Загрузка классов по имени",false);
        }
        // Native method flags in class_data, rather than matching words in strings.
        int count=d.u32(96),off=d.u32(100);d.table(off,count,32);
        for(int i=0;i<count;i++){
            int at=d.u32(off+i*32+24);if(at==0)continue;
            int[] cursor={at};int sf=d.uleb(cursor),inf=d.uleb(cursor),dm=d.uleb(cursor),vm=d.uleb(cursor);
            if((long)sf+inf+dm+vm>1000000)throw new IOException("Некорректная таблица DEX");
            for(int j=0;j<sf+inf;j++){d.uleb(cursor);d.uleb(cursor);}
            for(int j=0;j<dm+vm;j++){d.uleb(cursor);int flags=d.uleb(cursor);d.uleb(cursor);if((flags&256)!=0)report.add("Нативные методы вне проверки Java-кода",true);}
        }
        inspectNames(d,report,count,off);
        report.digest=sha256(data);return report;
    }
    private static final Set<String> SHORT_NAMES=new HashSet<String>(Arrays.asList("UI","IO","Db","id","of","is","eq","on","go","by","to","at","up","dx","dy","x","y","z","r","g","b"));
    private static boolean opaqueName(String name){
        if(name.isEmpty())return true;
        for(int i=0;i<name.length();i++)if(Character.isISOControl(name.charAt(i))||Character.getType(name.charAt(i))==Character.FORMAT)return true;
        return (name.length()<=2 && !SHORT_NAMES.contains(name)) || name.matches("[a-fA-F0-9]{16,}");
    }
    private static void inspectNames(Dex d,Report report,int count,int off)throws IOException{
        Set<Integer> own=new HashSet<Integer>();
        for(int i=0;i<count;i++){
            int at=off+i*32,index=d.u32(at);own.add(index);String type=d.type(index);
            String leaf=type.substring(type.lastIndexOf('/')+1,type.length()-1);
            // Inner classes generated by javac/kotlinc retain the enclosing readable name.
            String base=leaf.split("\\$",2)[0];
            if(opaqueName(base))report.add("Непрозрачные имена классов: обфускация запрещена",true);
            int source=d.raw32(at+16);
            if(source==-1)report.add("Удалены имена исходных файлов: публикация такого кода запрещена",true);
            else {String file=d.string(source);String stem=file.replaceFirst("\\.(java|kt)$","");if(opaqueName(stem)||(!file.endsWith(".java")&&!file.endsWith(".kt")))report.add("Непроверяемое имя исходного файла",true);}
        }
        for(int i=0;i<d.methodCount;i++){
            int at=d.methodOff+i*8;String name=d.string(d.u32(at+4));
            if(own.contains(d.u16(at))&&!name.startsWith("<")&&opaqueName(name))report.add("Непрозрачные имена методов: обфускация запрещена",true);
        }
        for(String text:d.decoded.values()){
            if(text.length()>256 && text.matches("[A-Za-z0-9+/=_-]+") && !embeddedImage(text))report.add("Закодированная строка похожа на скрытый код. Картинки (PNG/JPEG/WebP) разрешены, остальное нет",true);
        }
    }

    private static final class Dex {
        final byte[] b;private final Map<Integer,String> decoded=new HashMap<Integer,String>();private int textBytes,ulebReads;final int stringCount,stringOff,typeCount,typeOff,methodCount,methodOff;
        Dex(byte[] bytes)throws Exception{
            b=bytes;
            if(b.length<112||b.length>MAX_DEX||b[0]!='d'||b[1]!='e'||b[2]!='x'||b[3]!=10||b[7]!=0)throw new IOException("Некорректный DEX");
            String version=new String(b,4,3,"US-ASCII");if(!Arrays.asList("035","037","038","039","040").contains(version))throw new IOException("Неподдерживаемая версия DEX");
            if(u32(32)!=b.length||u32(36)!=112||u32(40)!=0x12345678)throw new IOException("Некорректный заголовок DEX");
            MessageDigest sha=MessageDigest.getInstance("SHA-1");sha.update(b,32,b.length-32);
            if(!Arrays.equals(sha.digest(),Arrays.copyOfRange(b,12,32)))throw new IOException("Повреждён DEX: SHA-1");
            Adler32 checksum=new Adler32();checksum.update(b,12,b.length-12);
            if(checksum.getValue()!=(raw32(8)&0xffffffffL))throw new IOException("Повреждён DEX: checksum");
            stringCount=u32(56);stringOff=u32(60);typeCount=u32(64);typeOff=u32(68);methodCount=u32(88);methodOff=u32(92);
            table(stringOff,stringCount,4);table(typeOff,typeCount,4);table(methodOff,methodCount,8);
            // Ensure all string entries are bounded, including unused entries.
            for(int i=0;i<stringCount;i++)string(i);
        }
        int raw32(int p)throws IOException{range(p,4);return (b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16)|((b[p+3]&255)<<24);}
        int u32(int p)throws IOException{int value=raw32(p);if(value<0)throw new IOException("Слишком большое значение DEX");return value;}
        int u16(int p)throws IOException{range(p,2);return (b[p]&255)|((b[p+1]&255)<<8);}
        void range(int p,long n)throws IOException{if(p<0||n<0||(long)p+n>b.length)throw new IOException("Таблица за границами DEX");}
        void table(int off,int count,int size)throws IOException{if(count>100000||(count==0)!=(off==0))throw new IOException("Некорректная таблица DEX");range(off,(long)count*size);}
        int uleb(int[] pos)throws IOException{if(++ulebReads>1000000)throw new IOException("Слишком сложный DEX для проверки");long value=0;for(int i=0;i<5;i++){range(pos[0],1);int v=b[pos[0]++]&255;value|=(long)(v&127)<<(i*7);if((v&128)==0){if(value>Integer.MAX_VALUE)throw new IOException("Переполнение DEX");return (int)value;}}throw new IOException("Некорректный ULEB128");}
        String string(int index)throws IOException{
            if(index<0||index>=stringCount)throw new IOException("Индекс строки DEX");
            int offset=u32(stringOff+index*4);String ready=decoded.get(offset);if(ready!=null)return ready;
            int[] pos={offset};uleb(pos);int start=pos[0];
            while(true){range(pos[0],1);if(b[pos[0]++]==0)break;if(++textBytes>MAX_DEX||pos[0]-start>65536)throw new IOException("Слишком большие строки DEX");}
            ready=new String(b,start,pos[0]-start-1,java.nio.charset.StandardCharsets.UTF_8);decoded.put(offset,ready);return ready;
        }
        String type(int index)throws IOException{if(index<0||index>=typeCount)throw new IOException("Индекс типа DEX");return string(u32(typeOff+index*4));}
    }
}
