
import java.io.BufferedReader;
import java.io.FileInputStream;
import javax.crypto.BadPaddingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
/**
 * 此class负责控制二维码生成以及验证,
 * 此Demo预先生成了若干二维码在文件夹Gen内，若想重新生成请删除二维码以及RSApriKey.txt /RSApubKey.txt /DesKey.txt内的所有内容.
 * 2019-8-27 Albert
 */
public class Verify {

    public static DESUtils DES= new DESUtils();
    public static RSAUtils RSA = new RSAUtils();
    public static Ceasar ceasar = new Ceasar();
    /**
     * 生成num个二维码
     * @param  num numbers of QRcode generated.
     * @return void
     */
    public static void getQRcode(int num)throws Exception{
        if(num>0) {
            for (int i = 0; i < num; i++) {
                Crypt crypt = new Crypt();
                crypt.main();
            }
        }
    }
    /**
     * 判断当前选中二维码是否符合防伪标准
     * 当前使用记事本充当服务器.
     * Keyfilename:储存RSA私钥(服务器端)
     * Textfilename:储存加密后的DES密匙(服务器端)
     * @param Keyfilename 私钥文件路径
     * @param Textfilename  密匙文件路径
     * @param filename 解密二维码文件名
     * @return boolean
     */
    public static boolean If_True(String Keyfilename,String Textfilename,String filename)throws Exception{
        //初始化两个bufferedReader
            BufferedReader br = getBufferReader(Keyfilename);
            BufferedReader br1 = getBufferReader(Textfilename);
            String nextline;
            String nextline1;
            String deskey = null;
        //对密文（DES密匙）进行RSA解密
        try {
            QRCodeUtil QRcode = new QRCodeUtil();
            String QRcodetext = QRcode.decode(filename);
            System.out.println("读取二维码信息成功！");
            int num=0;
            int i=0;
            for(;i<QRcodetext.length();i++) {
                char c=QRcodetext.charAt(i);
                if(c=='\r'){break;}
            }
            String text = QRcodetext.substring(0,i);
            String codedKey = QRcodetext.substring(i+2);
            System.out.println("加密原文text:  "+text);
            System.out.println("加密内容key:   "+codedKey);
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("是否使用DES加密并且使用RSA加密DES密匙？若是请输入Yes,若不是请输入任意字符跳过...");
            Scanner scan = new Scanner(System.in);
            if(scan.nextLine().equalsIgnoreCase("YES")){
                System.out.println("----------------------正在使用RSA解密二维码密匙----------------------------");
                while ((nextline = br.readLine()) != null) {
                    if (RSAdecrypt(nextline.substring(7), codedKey)) {
                        deskey = RSA.decryptData(nextline.substring(7), codedKey, false);//相当于服务器
                        System.out.println("----->解密成功！  密匙为:  " + deskey);
                        num=-1;
                        System.out.println("-------------------------------------------------------------------------");
                        break;
                    } else { num+=1;System.out.println("----->解密失败！使用记事本中第"+num+"个密匙"); }
                }
            }
            else{
                System.out.println("是否由DES加密二维码？若是请输入Yes,若由RSA加密请输入任意字符跳过...");
                Scanner scan1 = new Scanner(System.in);
                if(scan1.nextLine().equalsIgnoreCase("YES")){
                     deskey=codedKey;
                }
                else{
                    System.out.println("------------------------------正在使用RSA解密----------------------------");
                    num=0;
                    codedKey = QRcodetext.substring(0,i);
                    while ((nextline = br.readLine()) != null) {
                        if (RSAdecrypt(nextline.substring(7), codedKey)) {
                            deskey = RSA.decryptData(nextline.substring(7), codedKey, false);//相当于服务器
                            System.out.println("----->解密成功！  明文为:  " + deskey);
                            String ciphertext = deskey.substring(0,deskey.length()-3);
                            String swift = deskey.substring(deskey.length()-3);
                            StringBuffer jiemi = ceasar.deciphering(ciphertext,Integer.parseInt(swift));
                            System.out.println("----->重排列成功！原文为:  " + jiemi.toString());
                            System.out.println("-------------------------------------------------------------------------");
                            return true;
                        } else { num+=1;System.out.println("----->解密失败！使用记事本中第"+num+"个密匙"); }
                    }

                }
            }
            System.out.println("-------------------------------正在匹配DES密匙----------------------------");
            //若密匙成功解密，则使用此密匙对加密密文进行DES解密.
           while ((nextline1=br1.readLine())!=null){
                if(deskey.equals(nextline1.substring(7))){
                    System.out.println("-------------------------------------------------------------------------");
                    System.out.println("密匙成功匹配！开始进行DES解密..." );
                    String plaintext = DESdecrypt(text,deskey);
                    System.out.println("----->解密成功！  明文为:  " + plaintext);
                    //根据偏移量对明文前半部分进行重排列.
                    String ciphertext = plaintext.substring(0,plaintext.length()-3);
                    String swift = plaintext.substring(plaintext.length()-3);
                    StringBuffer jiemi = ceasar.deciphering(ciphertext,Integer.parseInt(swift));
                    System.out.println("----->重排列成功！原文为:  " + jiemi.toString());
                    System.out.println("-------------------------------------------------------------------------");
                    return true;
                }
                else { num+=1;System.out.println("----->匹配失败！使用记事本中第"+num+"个密匙"); }
            }
        }
        catch(IOException e){
            System.out.println("文件路径错误!");
        }
        catch(NullPointerException e){
            System.out.println("NullPointerException");
        }
        return false;
    }

    /**
     * DES解密单元
     * @param text
     * @param key
     * @return String
     */
    private static String DESdecrypt(String text,String key){
        try {

           String plaintext= DES.getDecryptString(text,key);
            return plaintext;
        }
        catch(Exception e){
            System.out.println("解密失败！");
            return null;
        }
    }
    /**
     * RSA解密单元
     * @param text
     * @param key
     * @return boolean
     */
    private static boolean RSAdecrypt(String key,String text){
        try {
            RSAUtils RSA = new RSAUtils();
            RSA.decryptData(key, text, false);
            return true;
        }
        catch(Exception e){
            return false;
        }
    }
    /**
     * 生成BufferReader,读取当前文件.
     * @param filename
     * @return BufferedReader
     * @throws Exception
     */
    public static BufferedReader getBufferReader(String filename)throws Exception{
        FileInputStream fis; // 创建FileInputStream类对象读取File
        InputStreamReader isr; // 创建InputStreamReader对象接收文件流
        BufferedReader br;
        fis = new FileInputStream(filename);
        isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        br = new BufferedReader(isr);
        return br;
    }
    /**
     * 判断产品真伪
     * @param keyfilename,Textfilename,filename
     * @return void
     * @throws Exception
     */

    private static void verify(String keyfilename,String Textfilename,String filename)throws Exception{

        Boolean if_true = If_True(keyfilename,Textfilename,filename);
        if(if_true){
            System.out.println("----->您的产品是正品.");
        }
        else{
            System.out.println("----->您的产品是赝品.");
        }
    }
    /**
     * 如要生成二维码，请注释掉//验证二维码后的部分.
     * 如需验证二维码，请注释掉//生成二维码后的部分，并在第三个parameter内修改二维码文件名.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)throws Exception{
        //生成二维码
        //getQRcode(1);
        //验证二维码
        /**
         * **************************************************************************************************************************
         * 请在此处添加您想要验证的QRcode文件名.
         * 当前生成二维码后存放文件路径为"F:\\Work_Space\\JAVA DES\\Gen",可以根据需要在QRCodeUtil.java内更改.
         * **************************************************************************************************************************
         */
        verify("F:\\Work_Space\\JAVA DES\\Demo\\RSApriKey.txt","F:\\Work_Space\\JAVA DES\\Demo\\DesKey.txt","398411002.jpg");
    }
}
