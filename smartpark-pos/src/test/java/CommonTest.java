import com.iandtop.common.utils.BinaryUtil;

/**
 * Created by Administrator on 2016-12-01.
 */
public class CommonTest {
    public static void main(String[] args) {
        String aaa = BinaryUtil.bytesToHexString(new byte[]{0x01, 0x02});
        System.out.print(aaa);
    }
}
