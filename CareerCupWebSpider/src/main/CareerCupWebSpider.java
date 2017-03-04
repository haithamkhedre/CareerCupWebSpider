package main;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A Spider that crawl career cup web site and slice questions to company / year
 * refresh your workspace to see java files ready to solve.
 *
 *@author Haitham Khedre
 *@email haitham.khedre@gmail.com
 */
public class CareerCupWebSpider {

	static String path ="./src/";
	static List<String> targets = Arrays.asList("microsoft","google","facebook","amazon","apple","oracle","uber");
	public static void main(String[] args) throws IOException {
		saveQuestions();
		saveTotals();
		solve();
	}

	private static void saveTotals() {
		appendTotals("==== ===== ===== Date :"+new Date().toString()+" ==== ===== ===== ");
		int total = 0;
		Set<String> fprints = null;
		for(String t : targets){
			fprints = getFingerPrints(t);
			total+=fprints.size();
			appendTotals(t +" : "+customFormat("###,###",fprints.size()));
		}
		appendTotals("total : "+customFormat("###,###",total));
	}

	private static void saveQuestions() throws IOException {
		for(String target : targets){
			crawl("https://www.careercup.com/page?pid=$-interview-questions",target);
			System.out.println("==== Finished : "+target+"=======");
		}
		
		System.out.println("==========   Done   ==========");
	}

	@SuppressWarnings("unchecked")
	public static void crawl(String URL,String target) throws IOException {
		List<String> urls = new ArrayList<String>();
		Set<String> visited = new HashSet<String>();
		Set<String> fingerPrints = null;
		Set<String> urlsMap = new HashSet<String>();
		URL = URL.replace("$", target);
		urls.add(URL);
		String urlFilter = URL.split("page?")[1];
		int count =0;
		
		while (urls.size() > 0) {
			fingerPrints = getFingerPrints(target);
			count =fingerPrints.size();
			String urlToSearch = urls.get(0);
			System.out.println("****Processing : "+ urlToSearch + "**************");
			visited.add(urlToSearch);
			// connect and get ur with timeout 10 seconds
			Document doc = null;
			boolean excpetionThrown = true;
			while (excpetionThrown) {
				try {
					doc = Jsoup.connect(urlToSearch).timeout(10000).get();
					break;
				} catch (Exception ex) {
					System.out.println("retrying : "+ urlToSearch);
				}
			}
			
			saveQuestions(target, fingerPrints, count, doc);
			
			urls.remove(0);
			Elements urlsToSearch = doc.select("a[href]");
			
			for (Element link : urlsToSearch) {
				if (link.attr("href").contains(urlFilter)) {
					String urlToPush = link.attr("abs:href").toString();
					if (!visited.contains(urlToPush) && !urlsMap.contains(urlToPush)) {
							urls.add(urlToPush);
							urlsMap.add(urlToPush);
					}
				}
			}
			Collections.sort(urls, new AlphanumComparator());
		}
	}

	private static void saveQuestions(String target, Set<String> fingerPrints,
			int count, Document doc) {
		Elements questions = doc.select("li");
		for (Element q : questions) {
			Elements lnk = q.select("a");
			Elements date  = q.select("span.author");
			String url = lnk.attr("abs:href");
			if (url.contains("question?id")) {
				StringBuilder sb = new StringBuilder();
				String fingerPrint = url.split("id=")[1];
				// we don't want to download old questions
				if(fingerPrints.contains(fingerPrint)) {
					continue;
				}
				sb.append(url+"\n");
				String dateDigit = date.text().replace("| Report Duplicate | Flag", "\n");
				sb.append(dateDigit);
				dateDigit  = "20"+dateDigit.split(", 20")[1].substring(0, 2);
				sb.append(lnk.text().replace("Report Duplicate Flag", "").replace("Answers", "Answers\n"));
				count++;
				if (saveQuestion(target, count,dateDigit,
						sb.toString())) {
					appendFingerPrint(target, fingerPrint);
					System.out.println("Downloading :" + url + " , count :"
							+ count);
				}
			}
		}
	}

	private static void appendFingerPrint(String target, String fingerPrint) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter
			         (path + target+"/"+"FingerPrints.txt",true));
			         out.write(fingerPrint+"\n");
			         out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private static void appendTotals(String value) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter
			         (path+"totals.txt",true));
			         out.write(value+"\n");
			         out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static public String customFormat(String pattern, double value) {
		DecimalFormat myFormatter = new DecimalFormat(pattern);
		String output = myFormatter.format(value);
		return output;
	}
	
	private static Set<String> getFingerPrints(String target) {
		try {
			List<String> lst = Files.readAllLines(
					Paths.get(path + target +"/"+ "FingerPrints.txt"),
					Charset.defaultCharset());
			return new HashSet<String>(lst);
		} catch (IOException e) {
			return new HashSet<String>();
		}
	}

	private static boolean saveQuestion(String target, int count ,String date, String value) {
		try {
			if(!Files.exists(Paths.get(path+target+"/"+date))){
			Files.createDirectories(Paths.get(path+target+"/"+date));
			}
			
			File file = new File(path+target+"/"+date+"/Question"+count+".java");
			if(file.exists()){
				throw new IOException("file already exist");
			}
			
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write("/*");
			fileWriter.write(value.replace("*/", "#/").replace("/*", "/#"));
			fileWriter.write("*/");
			fileWriter.flush();
			fileWriter.close();
			return true;
		} catch (IOException e) {
			System.out.println("error:"+e.getMessage());
			return false;
		}
	}
	
	private static void solve() {
		// Supernova ai = AI.Platform.getBrain(new BrainConfiguration("blackhole"))
		// ai.solve
	}
}