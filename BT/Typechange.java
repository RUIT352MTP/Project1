package BT;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class Typechange {

	/* copied. Gotta change this */

    private final static char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };


    public static String bytesToURL(final byte[] byteArr) {

        final String hexString = bytesToHexStr(byteArr);

        final int len = hexString.length();
        final char[] charArr = new char[len + (len / 2)];
        int i = 0;
        int j = 0;
        while (i < len) {
            charArr[j++] = '%';
            charArr[j++] = hexString.charAt(i++);
            charArr[j++] = hexString.charAt(i++);
        }
        return new String(charArr);
    }

    protected static String bytesToHexStr(final byte[] byteArr) {

        final char[] charArr = new char[byteArr.length * 2];
        for (int i = 0; i < byteArr.length; i++) {
            final int val = (byteArr[i] & 0xFF);
            final int charLoc = i << 1;
            charArr[charLoc] = HEX_CHARS[val >>> 4];
            charArr[charLoc + 1] = HEX_CHARS[val & 0x0F];
        }
        final String hexString = new String(charArr);

        return hexString;
    }

    protected static String objectToStr(Object o){

        if(o instanceof Integer){
            return String.valueOf(o);
        } else if(o instanceof ByteBuffer){
            try {
                return new String(((ByteBuffer) o).array(),"ASCII");
            } catch (UnsupportedEncodingException e) {
                return o.toString();
            }
        }else if(o instanceof Map<?,?>){

            String retStr = "";
            for (Object name: ((Map<?, ?>) o).keySet()){
                String value = objectToStr(((Map<?, ?>) o).get(name));
                retStr += objectToStr(name) + ": " + value + "\n";
            }

            return retStr;
        }else if(o instanceof List){

            String retStr = "";
            for(Object elem: (List<?>)o){
                retStr += objectToStr(elem) + "\n";
            }
            return retStr;
        }
        return o.toString();
    }

}