package com.straw.nfcdemo;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private TextView tv;
	private Button readCardBtn, addBtn, minusBtn, modifyBtn;
	private MifareClassic mTag;
	private NfcB nfcbTag;
	private int flag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		initWidget();
		onNewIntent(getIntent());

	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			tv.setText("卡ID=" + ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] techList = tag.getTechList();

			ArrayList<String> list = new ArrayList<String>();
			for (String string : techList) {
				list.add(string);
				System.out.println("tech=" + string);
			}
			if (list.contains("android.nfc.tech.NfcB")) {
				nfcbTag = NfcB.get(tag);
				try {
					nfcbTag.connect();
					if (nfcbTag.isConnected()) {
						System.out.println("已连接");
						Toast.makeText(MainActivity.this, "身份证已连接", Toast.LENGTH_SHORT).show();
						new CommandAsyncTask().execute();

					}
					// nfcbTag.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (list.contains("android.nfc.tech.MifareClassic")) {
				mTag = MifareClassic.get(tag);
				if (mTag != null) {
					try {
						mTag.connect();
						if (mTag.isConnected()) {
							System.out.println("已连接");
						} else {
							Toast.makeText(MainActivity.this, "请贴卡后再操作", Toast.LENGTH_SHORT).show();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (flag == 0) {
					tv.setText(readTag(mTag));
				} else if (flag == 1) {
					addValue(mTag);
				} else if (flag == 2) {
					minusValue(mTag);
				} else if (flag == 3) {
					modifyControl(mTag);
				}
			}

		}
	}

	private String ByteArrayToHexString(byte[] inarray) { // converts byte
															// arrays to string
		int i, j, in;
		String[] hex = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
		String out = "";

		for (j = 0; j < inarray.length; ++j) {
			in = inarray[j] & 0xff;
			i = (in >> 4) & 0x0f;
			out += hex[i];
			i = in & 0x0f;
			out += hex[i];
		}
		return out;
	}

	public String readTag(MifareClassic mfc) {
		boolean auth = false;
		// 读取TAG

		try {
			String metaInfo = "";
			// Enable I/O operations to the tag from this TagTechnology object.
			int type = mfc.getType();// 获取TAG的类型
			int sectorCount = mfc.getSectorCount();// 获取TAG中包含的扇区数
			String typeS = "";
			switch (type) {
			case MifareClassic.TYPE_CLASSIC:
				typeS = "TYPE_CLASSIC";
				break;
			case MifareClassic.TYPE_PLUS:
				typeS = "TYPE_PLUS";
				break;
			case MifareClassic.TYPE_PRO:
				typeS = "TYPE_PRO";
				break;
			case MifareClassic.TYPE_UNKNOWN:
				typeS = "TYPE_UNKNOWN";
				break;
			}
			metaInfo += "卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共" + mfc.getBlockCount() + "个块\n存储空间: " + mfc.getSize() + "B\n";
			for (int j = 0; j < sectorCount; j++) {
				// Authenticate a sector with key A.

				auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
				int bCount;
				int bIndex;
				if (auth) {
					metaInfo += "Sector " + j + ":验证成功\n";
					// 读取扇区中的块
					bCount = mfc.getBlockCountInSector(j);
					bIndex = mfc.sectorToBlock(j);
					for (int i = 0; i < bCount; i++) {
						byte[] data = mfc.readBlock(bIndex);
						metaInfo += "Block " + bIndex + " : " + ByteArrayToHexString(data) + "\n";
						bIndex++;
					}
				} else {
					metaInfo += "Sector " + j + ":验证失败\n";
				}
			}
			// // 写数据
			// auth = mfc.authenticateSectorWithKeyA(1,
			// MifareClassic.KEY_DEFAULT);
			// if (auth) {
			// byte[] bb = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
			// 13, 14, 15, 16 };
			// mfc.writeBlock(5, bb);
			//
			// }
			System.out.println(metaInfo);
			return metaInfo;
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} finally {
			if (mfc != null) {
				try {
					mfc.close();
				} catch (IOException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
		return null;

	}

	private void addValue(MifareClassic mfc) {
		try {
			boolean auth = mfc.authenticateSectorWithKeyA(2, MifareClassic.KEY_DEFAULT);
			if (auth) {
				mfc.increment(8, 10);
			} else {
				Toast.makeText(MainActivity.this, "扇区1密码校验错误", Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (mfc != null) {
				try {
					mfc.close();
				} catch (IOException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void minusValue(MifareClassic mfc) {
		try {
			boolean auth = mfc.authenticateSectorWithKeyA(2, MifareClassic.KEY_DEFAULT);
			if (auth) {
				mfc.increment(8, 2);
			} else {
				Toast.makeText(MainActivity.this, "扇区1密码校验错误", Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (mfc != null) {
				try {
					mfc.close();
				} catch (IOException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void modifyControl(MifareClassic mfc) {
		try {
			boolean auth = mfc.authenticateSectorWithKeyA(2, MifareClassic.KEY_DEFAULT);
			if (auth) {

				byte[] keyA = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, };
				byte[] control = new byte[] { 0x08, 0x77, (byte) 0x8F, 0X69 };
				byte[] keyB = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, };

				ByteArrayBuffer buffer = new ByteArrayBuffer(16);
				buffer.append(keyA, 0, keyA.length);
				buffer.append(control, 0, control.length);
				buffer.append(keyB, 0, keyB.length);
				mfc.writeBlock(11, buffer.toByteArray());
			} else {
				Toast.makeText(MainActivity.this, "扇区1密码校验错误", Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (mfc != null) {
				try {
					mfc.close();
				} catch (IOException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void initWidget() {
		tv = (TextView) findViewById(R.id.main_tv);
		tv.setMovementMethod(new ScrollingMovementMethod());

		readCardBtn = (Button) findViewById(R.id.readCard);
		addBtn = (Button) findViewById(R.id.addValue);
		minusBtn = (Button) findViewById(R.id.minusValue);
		modifyBtn = (Button) findViewById(R.id.modifyControl);

		readCardBtn.setOnClickListener(this);
		addBtn.setOnClickListener(this);
		minusBtn.setOnClickListener(this);
		modifyBtn.setOnClickListener(this);
	}

	private boolean checkTag() {
		boolean flag = false;
		if (mTag != null) {
			try {
				mTag.connect();
				if (mTag.isConnected()) {
					System.out.println("已连接");
					flag = !flag;
					tv.setText(readTag(mTag));
				} else {
					Toast.makeText(MainActivity.this, "请贴卡后再操作", Toast.LENGTH_SHORT).show();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return flag;
	}

	class CommandAsyncTask extends AsyncTask<Integer, Integer, String> {

		@Override
		protected String doInBackground(Integer... params) {
			byte[] result = new byte[] {};
			StringBuffer sb = new StringBuffer();
			try {
				byte[] cmd = new byte[] { 0x05, 0x00, 0x00 };
				result = nfcbTag.transceive(cmd);
				sb.append("寻卡指令:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
				cmd = new byte[] { 0x1D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x01, 0x08 };
				result = nfcbTag.transceive(cmd);
				sb.append("选卡指令:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
				sb.append("读固定信息指令\n");

				cmd = new byte[] { 0x00, (byte) 0xA4, 0x00, 0x00, 0x02, 0x60, 0x02 };
				result = nfcbTag.transceive(cmd);
				sb.append("发:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
				cmd = new byte[] { (byte) 0x80, (byte) 0xB0, 0x00, 0x00, 0x20 };
				result = nfcbTag.transceive(cmd);
				sb.append("发:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
				cmd = new byte[] { 0x00, (byte) 0x88, 0x00, 0x52, 0x0A, (byte) 0xF0, 0x00, 0x0E, 0x0C, (byte) 0x89, 0x53, (byte) 0xC3,
						0x09, (byte) 0xD7, 0x3D };
				result = nfcbTag.transceive(cmd);
				sb.append("发:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
				cmd = new byte[] { 0x00, (byte) 0x88, 0x00, 0x52, 0x0A, (byte) 0xF0, 0x00, };
				result = nfcbTag.transceive(cmd);
				sb.append("发:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
				cmd = new byte[] { 0x00, (byte) 0x84, 0x00, 0x00, 0x08 };
				result = nfcbTag.transceive(cmd);
				sb.append("发:" + ByteArrayToHexString(cmd) + "\n");
				sb.append("收:" + ByteArrayToHexString(result) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return sb.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			tv.setText(result);
			try {
				nfcbTag.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.readCard:
			flag = 0;
			// if (checkTag()) {
			// tv.setText(readTag(mTag));
			// }
			break;
		case R.id.addValue:
			flag = 1;
			//
			if (checkTag()) {
				addValue(mTag);
			}
			break;
		case R.id.minusValue:
			flag = 2;

			if (checkTag()) {
				minusValue(mTag);
			}
			break;
		case R.id.modifyControl:
			flag = 3;
			if (checkTag()) {
				modifyControl(mTag);
			}
			break;
		default:
			break;
		}
	}
}
