package jp.dip.ysato.hibikiplayer;

import java.util.ArrayList;

import android.graphics.Bitmap;

public class ProgramBean {
	private String title;
	private Bitmap image;
	private int no;
	private String imageURL;
	private ArrayList<String> playlisturl;
	private String detail;
	private String comment;
	public ProgramBean(String detail, String title, Bitmap image, int no, String comment, ArrayList<String> playlisturl) {
		this.detail = detail;
		this.title = title;
		this.image = image;
		this.no = no;
		this.comment = comment;
		this.playlisturl = playlisturl;
	}
	public String title() {
		// TODO Auto-generated method stub
		return title;
	}

	public Bitmap image() {
		// TODO Auto-generated method stub
		return image;
	}

	public int no() {
		return no;
	}
	
	public String imageUrl() {
		return imageURL;
	}

	public String playlisturl(int no) {
		return playlisturl.get(no);
	}
	public String detail() {
		return detail;
	}
	public String comment() {
		return comment;
	}
	
}
