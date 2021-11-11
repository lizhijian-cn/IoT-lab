package com.topelec.buscard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.topelec.database.DatabaseHelper;
import com.topelec.service.CardService;

import it.moondroid.coverflowdemo.R;

public class ResumeActivity extends Activity {

    private static final String TAG = ".ResumeActivity";
    private static final double stepValue = 2.00;

    private ImageView statusView;
    private TextView idView;
    private TextView stepView;
    private TextView sumView;

    /**数据库相关**/
    Context mContext;
    DatabaseHelper mDatabaseHelper;
    SQLiteDatabase mDatabase;

    private final static String TABLE_NAME = "HFCard";
    private final static String ID = "_id";
    private final static String CARD_ID = "card_id";
    private final static String SUM = "sum";

    private CardService cardService = new CardService();
    /**
     * 0: 显示余额
     * 1: 购买待刷卡
     * 2: 已购买
     */
    private int purchaseState = 0;
    private int cost = 0;

    /**
     * 用于同步UI,接受CardActivityGroup的broadcast
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int what = intent.getExtras().getInt("what");
            switch (what) {
                case 1://初始化错误
                    //TODO:
                    showMsgPage(R.drawable.buscard_consume_check_wrong,intent.getExtras().getString("Result"),"","");
                    break;
                case 2://未检测到卡
                    hideMsgPage();
                    if (purchaseState == 2) {
                        resetPurchaseState();
                    }
                    break;
                case 3: //成功获取卡号
                    String cardNo = intent.getExtras().getString("Result");
                    if (!cardService.checkRegisterState(cardNo)) {
                        showMsgPage(R.drawable.buscard_consume_check_wrong,getResources().getString(R.string.buscard_please_author_first),"","");
                        return;
                    }
                    System.out.printf("%s %d %d\n", cardNo, purchaseState, cost);
                    if (purchaseState == 0) {
                        int balance = cardService.getBalance(cardNo);
                        showMsgPage(R.drawable.buscard_consume_check_right, cardNo, "0", String.valueOf(balance));
                    } else if (purchaseState == 1) {
                        int balance = cardService.consume(cardNo, cost);
                        showMsgPage(R.drawable.buscard_consume_check_right, cardNo, String.valueOf(cost), String.valueOf(balance));
                        purchaseState = 2;
                    } else {
                        int balance = cardService.getBalance(cardNo);
                        showMsgPage(R.drawable.buscard_consume_check_right, cardNo, String.valueOf(cost), String.valueOf(balance));
                    }
                    break;
                case 4: // 购买套餐
                    break;
                case 5: // 取消购买
                    break;
                default:
                    break;
            }
        }
    };

    // 多线程环境下，需要保证原子性
    private void purchase(int cost) {
        purchaseState = 1;
        this.cost = cost;
    }

    private void resetPurchaseState() {
        purchaseState = 0;
        cost = 0;
    }

    private void hideMsgPage(){

        statusView.setImageDrawable(null);
        idView.setText("");
        stepView.setText("");
        sumView.setText("");
    }
    private void showMsgPage(int imageId,String cardId,String stepNum,String sumNum){
        statusView.setImageDrawable(getResources().getDrawable(imageId));
        idView.setText(cardId);
        stepView.setText(stepNum);
        sumView.setText(sumNum);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume);


        /**数据库相关变量初始化**/
        mContext = this;
        mDatabaseHelper = DatabaseHelper.getInstance(mContext);
        mDatabase = mDatabaseHelper.getReadableDatabase();

        statusView = (ImageView)findViewById(R.id.resume_statusView);
        idView = (TextView)findViewById(R.id.resume_idView);
        stepView = (TextView)findViewById(R.id.stepView);
        sumView = (TextView)findViewById(R.id.resume_sumView);

        hideMsgPage();

        ImageButton btnBuy1 = (ImageButton) findViewById(R.id.btn_buy1);
        btnBuy1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(12);
            }
        });

        ImageButton btnBuy2 = (ImageButton) findViewById(R.id.btn_buy2);
        btnBuy2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(20);
            }
        });

        ImageButton btnBuy3 = (ImageButton) findViewById(R.id.btn_buy3);
        btnBuy3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(25);
            }
        });

        ImageButton btnBuy4 = (ImageButton) findViewById(R.id.btn_buy4);
        btnBuy4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                purchase(15);
            }
        });

        ImageButton btnCancel = (ImageButton) findViewById(R.id.btn_cancel_buy);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPurchaseState();
            }
        });
    }

    /**
     *
     * @param CardId 卡号
     */
    private void updateCardUI(String CardId) {
        String searchResult = searchHFCard(CARD_ID,CardId);
        if (searchResult == null || searchResult.length() <= 0) { //如果数据库中没有记录
            showMsgPage(R.drawable.buscard_consume_check_wrong,getResources().getString(R.string.buscard_please_author_first),"","");

        } else if (searchResult.equals("-1")) {  //返回值为-1，数据库中搜索不止一个记录，错误
            showMsgPage(R.drawable.buscard_consume_check_wrong,getResources().getString(R.string.buscard_search_more_than_one),"","");

        } else {  //返回金额，更新UI
            double newSum = Double.valueOf(searchResult) - stepValue;
            if (newSum < 0) {
                showMsgPage(R.drawable.buscard_consume_check_wrong,getResources().getString(R.string.buscard_shortage),"",searchResult);
            }else {
                if (Double.toString(newSum).equals(updateHFCard(CARD_ID, CardId, SUM, Double.toString(newSum)))) {
                    showMsgPage(R.drawable.buscard_consume_check_right,CardId,Double.toString(stepValue),Double.toString(newSum));
                }

            }

        }
    }
       /**
     * 查询一条记录
     * @param key
     * @param selectionArgs
     * @return 返回金额数值对应的字符串
     */
    private String searchHFCard(String key,String selectionArgs) {
        Cursor cursor = mDatabase.query(TABLE_NAME, new String[]{SUM}, key + "=?", new String[] {selectionArgs}, null, null,null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        //double[] sumList = new double[cursor.getCount()];
        if (cursor.getCount() == 1) {
            double sum = cursor.getDouble(0);
            cursor.close();
            return Double.toString(sum);
        }else if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        } else {
            for (int i = 0;i <cursor.getCount();i++)
            {
                Log.v(TAG, "Current cursor = " + Double.toString(cursor.getDouble(0)));
                cursor.moveToNext();
            }
            cursor.close();
            return "-1";
        }
    }

    /**
     * 更新一条记录
     * @param key
     * @param data
     * @return 返回充值后的金额金额字符串，错误返回null
     */
    private String updateHFCard(String key, String data,String Column, String value) {
        ContentValues values = new ContentValues();
        values.put(Column, value);
        int result =  mDatabase.update(TABLE_NAME, values, key + "=?",new String[]{data});
        if (result != 0) {
            return value;
        }
        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();

        /**用于接收group发送过来的广播**/
        /***用于接收group发送过来的广播***/
        IntentFilter filter = new IntentFilter(CardActivityGroup.resume_action);
        registerReceiver(broadcastReceiver,filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
    }
}
