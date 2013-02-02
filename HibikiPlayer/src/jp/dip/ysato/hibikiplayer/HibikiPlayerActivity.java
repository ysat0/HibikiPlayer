package jp.dip.ysato.hibikiplayer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jp.dip.ysato.hibikiplayer.R.id;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class HibikiPlayerActivity extends Activity {
    /* Called when the activity is first created. */
   private String dow[] = new String[6];
	private ViewFlipper viewflipper;
	private View[] innerView = new View[6]; 
	private ProgramAdapter[] adapters = new ProgramAdapter[6];
	private GestureDetector gestureDetector;
	private int page;
	private int progress;
	private ProgressDialog dialog;
	private HttpClient httpClient;
	private String errmsg;
	private boolean active[] = new boolean[6];
	private DocumentBuilder documentBuilder;
	private HashMap<String,Boolean> imgcache = new HashMap<String, Boolean>();
	private void RemoveTag(StringBuilder sb, String tag) {
		int start = 0;
		for (;;) {
			start = sb.indexOf(tag, start);
			if (start < 0)
					break;
			sb.replace(start, start + tag.length(), "");
		}
	}
	
	private Bitmap getImage(String imageurl) {
		String name[] = imageurl.split("/");
		String filename = name[name.length - 1];
		if (!imgcache.containsKey(filename)) {
			HttpGet get = new HttpGet(imageurl);
			HttpResponse response;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				response = httpClient.execute(get);
				response.getEntity().writeTo(out);
				OutputStream cache = openFileOutput(filename, Context.MODE_PRIVATE);
				BufferedOutputStream buf = new BufferedOutputStream(cache);
				buf.write(out.toByteArray(), 0, out.size());
				imgcache.put(filename, true);
				clearneterror();
				return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			networkerror();
			return null;
		} else {
			imgcache.put(filename, true);
			filename = getFilesDir() + "/" + filename;
			return BitmapFactory.decodeFile(filename);
		}
	}
	
	private void networkerror() {
		// TODO Auto-generated method stub
		errmsg  = getString(R.string.networkerror);
	}

	private String getprogram(int w) {
		HttpGet get = new HttpGet("http://hibiki-radio.jp/get_program/" + String.valueOf(w + 1));
		ByteArrayOutputStream html = new ByteArrayOutputStream();
		HttpResponse httpResponse;
		try {
			httpResponse = httpClient.execute(get);
			httpResponse.getEntity().writeTo(html);
			clearneterror();
			return html.toString("UTF-8");
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		networkerror();
		return null;
	}
	
	private void clearneterror() {
		// TODO Auto-generated method stub
		errmsg = null;
	}

	private void getElements(int w) {
		httpClient = new DefaultHttpClient();
		HttpParams param = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(param, 60 * 1000);
		HttpConnectionParams.setSoTimeout(param, 60 * 1000);
		param.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
		documentBuilder = null;
		try {
			documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (active[w] == true)
			return;
		StringBuilder stringBuilder = new StringBuilder("<html><body>");
		String html;
		do {
			html = getprogram(w);
		} while(html == null);

		stringBuilder.append(html);
		stringBuilder.append("</div></body></html>");
		RemoveTag(stringBuilder, "<br>");
		RemoveTag(stringBuilder, "<br />");
		RemoveTag(stringBuilder, "</a>");
		int a_start = 0;
		int a_end = 0;
		for (;;) {
			a_start = stringBuilder.indexOf("<a ", a_end);
			if (a_start < 0)
				break;
			a_end = stringBuilder.indexOf(">", a_start);
			stringBuilder.insert(a_end + 1, "</a>");
		}

		ArrayList<ProgramBean> programs = new ArrayList<ProgramBean>();

		for(;;) {
			try {
				documentBuilder.reset();
				Document document = documentBuilder.parse(new ByteArrayInputStream(stringBuilder.toString().getBytes()));
				NodeList nodeList = document.getElementsByTagName("div");
				for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
					Node div = nodeList.item(i1).getAttributes().getNamedItem("class");
					if(div != null && div.getNodeValue().equals("hbkProgram")) {
						NodeList hbkProgram = nodeList.item(i1).getChildNodes();
						String title = null;
						String imageurl = null;
						String comment = null;
						int no = 0;
						ArrayList<String> playlisturl;
						String detail = parseVideo(hbkProgram.item(0).getAttributes().getNamedItem("onClick").getNodeValue());
						imageurl = hbkProgram.item(1).getAttributes().getNamedItem("src").getNodeValue();
						title = hbkProgram.item(2).getTextContent();
						comment = hbkProgram.item(3).getTextContent();
						Pattern p = Pattern.compile("第([０-９]+)回");
						Matcher m = p.matcher(comment);
						if (m.find()) {
							no = Integer.parseInt(tohalf(m.group(1)));
						} else {
							p = Pattern.compile("第(\\d+)回");
							m = p.matcher(comment);
							if (m.find())
								no = Integer.parseInt(m.group(1));
							else {
								no = -1;
							}
						}
						playlisturl = getplaylist(detail);
						if (playlisturl.isEmpty())
							continue;
						ProgramBean programBean = new ProgramBean(detail, title, getImage(imageurl), no, comment, playlisturl);
						programs.add(programBean);
					}
				}
				break;
			} catch (SAXNotRecognizedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		active[w] = true;
		adapters[w].setList(programs);
	}

	private String tohalf(String wide) {
		StringBuffer sb = new StringBuffer();
		for (char c : wide.toCharArray()) {
			sb.append((char)(c - '０' + '0'));
		}
		return sb.toString();
	}

	private String parseVideo(String Value) {
		int s = Value.indexOf("'");
		int e = Value.indexOf("'", s + 1);
		return Value.substring(s + 1, e);
	}

	private ArrayList<String> getplaylist(String title) {
		String description = "http://hibiki-radio.jp/description/" + title;
		HttpGet get = new HttpGet(description);

		String html;
		for(;;) {
			try {
				HttpResponse response = httpClient.execute(get);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				clearneterror();
				html = out.toString("UTF-8");
				break;
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				networkerror();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				networkerror();
			}
		}
		BufferedReader reader = new BufferedReader(new StringReader(html));
		String line;
			
		ArrayList<String> urls = new ArrayList<String>();
		urls.clear();
		try {
			while((line = reader.readLine()) != null) {
				if (line.indexOf("<embed name=\"wmp2\"") >= 0) {
					while((line = reader.readLine()).indexOf("<div id=\"hbkFooter\">") < 0) {
						if (line.indexOf("src=") == 0) {
							int s = line.indexOf("\"");
							int e = line.indexOf("\"", s + 1);
							if (e > 0) 
								urls.add(line.substring(s + 1, e));
							else
								urls.add(line.substring(s + 1));
						}
						if (line.indexOf("<a href=") == 0) {
							int s = line.indexOf("\"");
							int e = line.indexOf("\"", s + 1);
							if (e > 0) 
								urls.add(line.substring(s + 1, e));
							else
								urls.add(line.substring(s + 1));
						}
					}
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		urls = getmeta(urls);
		return urls;
	}

   private ArrayList<String> getmeta(ArrayList<String> urls) {
		ArrayList<String> result = new ArrayList<String>();
		for(String url: urls) {
			if (url.indexOf(".aspx") > 0) {
				HttpGet get = new HttpGet(url);
				HttpResponse response;
				for(;;) {
					try {
						response = httpClient.execute(get);
						break;
					} catch (ClientProtocolException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try {
					response.getEntity().writeTo(out);
					clearneterror();
					String meta;
					meta = out.toString("UTF-8");
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder;
	
					builder = factory.newDocumentBuilder();
					Document doc;
	
					doc = builder.parse(new ByteArrayInputStream(meta.getBytes("UTF-8")));
					Element ref = (Element) doc.getElementsByTagName("Ref").item(0);
					if (ref != null)
						result.add(ref.getAttribute("href"));
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (url.indexOf(".m3u8") > 0) {
				HttpGet get = new HttpGet(url);
				HttpResponse response;
				for(;;) {
					try {
						response = httpClient.execute(get);
						break;
					} catch (ClientProtocolException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try {
					response.getEntity().writeTo(out);
					clearneterror();
					InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "UTF-8");
					BufferedReader reader = new BufferedReader(in);
					String line;
					while((line = reader.readLine()) != null) {
						if (line.indexOf("#") == 0)
							continue;
						result.add(line);				
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}				
		return result;
	}
   private void showPage(int w) {
	   View v = viewflipper.getCurrentView();
	   int i;
	   for (i = 0; i < 6; i++)
		   if (innerView[i] == v)
			   break;
	   if (w > i)
		   for (; i < w; i++)
			   viewflipper.showNext();
	   else	
		   for (; i > w; i--)
			   viewflipper.showPrevious();
   }
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.main);
       setupdow();
       initImageCache();
       viewflipper = (ViewFlipper) findViewById(id.viewFlipper1);
       LayoutInflater l = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		gestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				boolean ret =super.onFling(e1, e2, velocityX, velocityY);
				if (Math.abs(velocityY) < Math.abs(velocityX) && Math.abs(velocityX) > 10) {
					if (velocityX < 0) {
						viewflipper.setInAnimation(AnimationUtils.loadAnimation(HibikiPlayerActivity.this, R.anim.slide_in_right));
						do {
							viewflipper.showNext();
							page++;
							page %= 6;
						} while(adapters[page].isEmpty());
					} else {
						viewflipper.setInAnimation(AnimationUtils.loadAnimation(HibikiPlayerActivity.this, android.R.anim.slide_in_left));
						do {
							viewflipper.showPrevious();
							page--;
							if (page < 0)
								page = 5;
						} while(adapters[page].isEmpty());	
						viewflipper.showPrevious();
					}
					ret = true;
				}
				return ret;
			}
			@Override
			public boolean onDoubleTap(MotionEvent event) {
				ListView listView = (ListView) viewflipper.getCurrentView().findViewById(R.id.programList);
				int pos = listView.pointToPosition((int)event.getX(), (int)event.getY());
				if (pos == ListView.INVALID_POSITION)
					return false;
				else {
					ProgramAdapter adapter = (ProgramAdapter) listView.getAdapter();
					ProgramBean program = (ProgramBean) adapter.getItem(pos);
					Uri uri = Uri.parse("http://hibiki-radio.jp/description/" + program.detail());
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
					return true;
				}
			}
		});
       	
       for (int i = 0; i < 6; i++) {
       	innerView[i] = l.inflate(R.layout.programlist, null);
       	TextView t = (TextView) innerView[i].findViewById(R.id.dayOfWeek);
       	t.setText(dow[i]);
       	viewflipper.addView(innerView[i]);
       	adapters[i] = new ProgramAdapter(this);
       	ListView list = (ListView) innerView[i].findViewById(R.id.programList);
       	list.setAdapter(adapters[i]);
       	list.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View arg0, MotionEvent arg1) {
					// TODO Auto-generated method stub
					gestureDetector.onTouchEvent(arg1);
					return false;
				}
       	});
       }
       if (dialog == null) {
    	   dialog = new ProgressDialog(this);
    	   dialog.setCancelable(false);
    	   dialog.setMessage(getString(R.string.loadingProgram));
    	   dialog.show();
       }
       final Handler handler = new Handler();

	final Runnable updateDataset = new Runnable() {
    	   @Override
    	   public void run() {
    		   // TODO Auto-generated method stub
		       ProgressBar progressbar = (ProgressBar)findViewById(R.id.progressBar1);
		       progressbar.setProgress(progress++);
		       adapters[page].notifyDataSetChanged();
				if (dialog != null) {
					dialog.dismiss();
					dialog = null;
				}
    	   }
    	   
       };
       final Runnable hideprogress = new Runnable() {
    	   @Override
    	   public void run() {
    		   // TODO Auto-generated method stub
    		   ProgressBar progressbar = (ProgressBar)findViewById(R.id.progressBar1);
    		   progressbar.setVisibility(View.GONE);
    		   clearImgCache();
    	   }
       };

       Calendar cal = new GregorianCalendar();
		page = cal.get(Calendar.DAY_OF_WEEK) - 2;
		if (page < 0 || page > 5)
			page = 5;
		showPage(page);
		
		new Thread(new Runnable() {
    	   @Override
    	   public void run() {
    			for(int i = 0; i < 6; i++) {
    				getElements(page);
    				handler.post(updateDataset);
    				page++;
    				page %= 6;
    			}
				handler.post(hideprogress);
			}
		}).start();
   }
	protected void clearImgCache() {
		for (String f: imgcache.keySet()) {
			if (!imgcache.get(f))
				deleteFile(f);
		}
	}

	private void initImageCache() {
		for (String f: fileList()) {
			imgcache.put(f, false);
		}
	}

	private void setupdow() {
		// TODO Auto-generated method stub
		Calendar cal = new GregorianCalendar();
		for (int i = 0; i < 7; i++) {
			int dow = cal.get(Calendar.DAY_OF_WEEK);
			if (dow >= Calendar.MONDAY && dow <= Calendar.SATURDAY)
				this.dow[dow - Calendar.MONDAY] = String.format("%1$tA", cal);
			cal.add(Calendar.DAY_OF_WEEK, 1);
		}
	}
}