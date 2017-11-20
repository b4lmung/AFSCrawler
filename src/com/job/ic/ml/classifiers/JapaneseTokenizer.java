package com.job.ic.ml.classifiers;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import net.moraleboost.io.BasicCodePointReader;
import net.moraleboost.tinysegmenter.TinySegmenter;
import net.moraleboost.tinysegmenter.TinySegmenter.Token;

public class JapaneseTokenizer extends MyTextTokenizer implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5740403022185489186L;
	private static String[] words = {"の","に","は","を","た","が","で","て","と","し","れ","さ","ある","いる","も","する","から","な","こと","として","い","や","れる","など","なっ","ない","この","ため","その","あっ","よう","また","もの","という","あり","まで","られ","なる","へ","か","だ","これ","によって","により","おり","より","による","ず","なり","られる","において","ば","なかっ","なく","しかし","について","せ","だっ","その後","できる","それ","う","ので","なお","のみ","でき","き","つ","における","および","いう","さらに","でも","ら","たり","その他","に関する","たち","ます","ん","なら","に対して","特に","せる","及び","これら","とき","では","にて","ほか","ながら","うち","そして","とともに","ただし","かつて","それぞれ","または","お","ほど","ものの","に対する","ほとんど","と共に","といった","です","とも","ところ","ここ","。"};
	private static HashSet<String> stopwords = new HashSet<>(Arrays.asList(words));
	private TinySegmenter ts;
	private ArrayList<String> tokens;
	private Iterator<String> it;
	
	
	
	public static void main(String[] args){
		String target = "みどころ堀井新太が民放連続ドラマで初主演するハートフルコメディー。ひょんなことから赤ちゃんを育てることになった3人の若者たちが、子育てを通して、目の前の命と向き合い成長する姿をコミカルに描く。堀井が演じるのは高校時代の友人らとシェアハウスで暮らす、明るく能天気な主人公・平林拓人。共にシェアハウスで暮らす友人で3人のリーダー的存在・羽野恭平を山田裕貴、アパレル勤務のクールでマイペースな性格の岡山朔を三津谷亮が演じる。そのほか、松井愛莉、相楽樹が出演。シェアハウスの大家・大山美奈子を演じる濱田マリが、フレッシュな顔ぶれを役同様に支える。";
		
		JapaneseTokenizer t = new JapaneseTokenizer();
		t.tokenize(target);
		
		while(t.hasMoreElements()){
			System.out.println(t.nextElement());
		}
		
	}

	@Override
	public String getRevision() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String globalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasMoreElements() {
		// TODO Auto-generated method stub
		return it.hasNext();
	}

	@Override
	public String nextElement() {
		// TODO Auto-generated method stub
		return it.next();
	}

	@Override
	public void tokenize(String arg0) {
		ts = new TinySegmenter(new BasicCodePointReader(new StringReader(arg0)));
		tokens = new ArrayList<>();
		Token token;
		
		try {
			while( (token=ts.next()) != null){
				if(!stopwords.contains(token.str))
					tokens.add(token.str);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		it = tokens.iterator();
	}
	
	
	public ArrayList<String> tokenizeString(String input){
		tokenize(input);
		return this.tokens;
	}
	
	@Override
	public TokenizedOutput tokenizeString(String input, boolean incNonWord) {
		TokenizedOutput to = new TokenizedOutput();
		tokenize(input);
		to.setTokenized(this.tokens);
		return to;
	}
}
