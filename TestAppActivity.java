package com.snapfish.test.ui;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.snapfish.android.generated.bean.MediaItem;
import com.snapfish.checkout.Image;
import com.snapfish.checkout.Image.ImageType;
import com.snapfish.checkout.Merchandise;
import com.snapfish.checkout.PartnerCredentials;
import com.snapfish.checkout.SFException;
import com.snapfish.checkout.SnapfishCheckout;
import com.snapfish.checkout.UserData;
import com.snapfish.internal.api.CSFNetworkUtils;
import com.snapfish.internal.api.OAuthResource;
import com.snapfish.internal.auth.SnapfishOauth;
import com.snapfish.internal.core.data.ContextData;
import com.snapfish.internal.core.data.OAuthData;
import com.snapfish.internal.core.data.OAuthResponse;
import com.snapfish.internal.datamodel.CAccountManager;
import com.snapfish.products.IPrint.PaperFinish;
import com.snapfish.products.SnapfishProductFactory;
import com.snapfish.products.impl.CPrint;
import com.snapfish.util.CLogger;

public class TestAppActivity extends Activity {
    private static final String ENV_LAST_TIME_EXECUTED = "ENV_LAST_TIME_EXECUTED";
private static final CLogger sLogger = CLogger.getInstance(TestAppActivity.class.getName());
    protected static final String SF_LOGIN_USERNAME = "SF_LOGIN_USERNAME";
    protected static final String SF_LOGIN_PASSWORD = "SF_LOGIN_PASSWORD";
    private ArrayList<String> imageHRUris;
    private ArrayList<String> imageTNUris;
    private String m_key = null;
    private String m_secret = null;
    private String m_urlEndpoint = null;
    private String m_product = null;
    private enumLoginType m_loginType = enumLoginType.SNAFISH;
    private Image.ImageType m_imageType = ImageType.LOCAL;
    private ProgressDialog m_progressDialog;
    private static final int REQUEST_SNAPFISH_CHECKOUT = 100;
    UserData userData = new UserData();
    private static String envSelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_app);
        setDefaultEnvDetails();
        setDefaultProduct();
        defaultLoginType();
        RadioGroup guestLoginRadioGroup = updateGuestLoginSectionProperties();
        addOnclickListnerForSnapfishLoginButton();
        addOnclickListnerForReturningButton();
        addOnclickListnerForGuestButton();
        addOnClickLocalImagesButton();
        addOnClickSnapfishImagesButton();
        addOnClickRemoteImagesButton();
        addOnClickRemoteImageUrlLabels();
        addOnClickPrintButton();
    }

    private void addOnClickRemoteImageUrlLabels() {
// final ClipboardManager clipMan = (ClipboardManager)getSystemService(getApplicationContext().CLIPBOARD_SERVICE);
// final TextView textViewUrl1 = (TextView)findViewById(R.id.remoteUrl1);
// textViewUrl1.setOnClickListener(new View.OnClickListener() {
//
// @Override
// public void onClick(View arg0) {
// sLogger.debug("The vlaue of copy value is "+clipMan.getText());
//
// EditText text = (EditText)findViewById(R.id.editRemoteImageUrl1);
// text.setText(clipMan.getText());
// }
// });
// final TextView textViewUrl2 = (TextView)findViewById(R.id.remoteUrl2);
// textViewUrl2.setOnClickListener(new View.OnClickListener() {
//
// @Override
// public void onClick(View arg0) {
// sLogger.debug("The vlaue of copy value is "+clipMan.getText());
// EditText text = (EditText)findViewById(R.id.editRemoteImageUrl2);
// text.setText(clipMan.getText());
//
// }
// });
//

}

private void addOnClickPrintButton() {
        
        final Button btnPrint = (Button)findViewById(R.id.btnPrint);
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                m_progressDialog = ProgressDialog.show(TestAppActivity.this,
                        "Please wait...", "launching Snapfish SDK...", true, true);
                final Map<String, PartnerCredentials> appKeySecret = new HashMap<String, PartnerCredentials>();
                Thread t = createThreadToLauchSDK(v);
                t.start();
            }

            private Thread createThreadToLauchSDK(final View v) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CPrint print = null;
                        List<Merchandise> allProducts = null;
                        try {
                            buildLoginForPrint(v);
                            List<Image> selectedImages = buildImagesForPrint();
                            SnapfishCheckout sc = new SnapfishCheckout("snapfish_us", new Locale("en", "US"), m_key, m_secret,
                                    new UserData(),
                                    m_urlEndpoint,
                                    v.getContext());
                            
                            allProducts = sc.getSupportedMerchandise();
                            for(Merchandise m : allProducts) {
                                boolean supportedMrch = false;
                                if(String.valueOf(m.getMrchId()).equals(m_product)) {
                                    try {
                                        System.out.println("creating builder for mrchId"+m.getMrchId());
                                        print = SnapfishProductFactory.create(m);
                                        supportedMrch = true;
                                    } catch (SFException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            
                            
                            print.setImages(selectedImages);
                            //set the paper type attribute for the print
                            print.setPaperFinish(PaperFinish.GLOSSY);
                            //set the number of prints to be ordered
                            print.setQuantity(1);
                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            Editor editor = pref.edit();
                            editor.putString(ENV_LAST_TIME_EXECUTED, envSelected);
                            editor.commit();
                             /* Call mail order function for the product. This launches the SDK*/
                            Intent intent = sc.createOrderIntent(print);
                            TestAppActivity.this.startActivityForResult(intent, REQUEST_SNAPFISH_CHECKOUT);
                            m_progressDialog.dismiss();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            m_progressDialog.dismiss();
                        }
                    }
                    
                });
                return t;
            }
        });
        
    }
    
    private void buildLoginForPrint(View v) {
        
        SnapfishOauth oauth = null;
        sLogger.debug("while buildLoginForPrint the value of m_urlEndpoint "+m_urlEndpoint);
        oauth = SnapfishOauth.getInstance(m_key, m_secret, m_urlEndpoint);
        ContextData.init(v.getContext());
        if(m_urlEndpoint.equals("https://openapi.sfstg8.qa.snapfish.com") ||
                m_urlEndpoint.equals("https://openapi.sfmir1.qa.snapfish.com") )
        {
            SnapfishCheckout.IS_PRODUCTION = false;
            SnapfishCheckout.IS_SANDBOX = false;
        }
        else
        {
            SnapfishCheckout.IS_PRODUCTION = true;
        }
        
        if(m_loginType.equals(enumLoginType.SNAFISH))
        {
            String userId = ((EditText) findViewById(R.id.editUserId)).getText().toString();
            String password = ((EditText) findViewById(R.id.editPasswd)).getText().toString();
            
            if(userId.isEmpty() && password.isEmpty())
            {
                ((TextView)findViewById(R.id.textUserId)).setTextColor(Color.RED);
                ((TextView)findViewById(R.id.textPassword)).setTextColor(Color.RED);
                throw new RuntimeException("Both Userid and Password is mandatory");
            }
            else if(userId.isEmpty() )
            {
                ((TextView)findViewById(R.id.textUserId)).setTextColor(Color.RED);
                throw new RuntimeException("UserName is mandatory");
            }
            else if(password.isEmpty())
            {
                ((TextView)findViewById(R.id.textPassword)).setTextColor(Color.RED);
                throw new RuntimeException("Password is mandatory");
            }
            oauth.cleanPreviousAuthInfoFromPreferences(v.getContext());
            oauth.refreshKeys( m_key, m_secret, m_urlEndpoint);
            OAuthResponse resp = OAuthResource.authenticateUser(v.getContext(), m_key, m_secret,
                    m_urlEndpoint, userId, password);
            oauth.initOAuthData(resp, false);
        }
        else if(m_loginType.equals(enumLoginType.GUEST))
        {
            oauth.cleanPreviousAuthInfoFromPreferences(v.getContext());
            oauth.refreshKeys( m_key, m_secret, m_urlEndpoint);
            
        }
        else if(m_loginType.equals(enumLoginType.RETURNING))
        {
            oauth.getOAuthData().setRefreshedInfo(null, null,
                    null, null, null);
            OAuthData data = oauth.getRefreshTokenFromPreferences();
            data.setAccessToken("");
            CSFNetworkUtils.refreshOAuth();
            
        }
    }
    
    private List<Image> buildImagesForPrint() throws Exception {
        List<Image> selectedImages = new ArrayList<Image>();
        if(m_imageType.equals(ImageType.LOCAL))
        {
            System.out.println("The value of images to "+imageHRUris);
            if(null == imageHRUris || imageHRUris.size() == 0)
            {
                String message = "Select Local/Remote/Snapfish Images";
                if(m_loginType.equals(enumLoginType.GUEST)){
                    message = "Select Local/Remote images";
                }
                throw new RuntimeException(message);
            }
            
            
            for(int imgHRURICount = 0; imgHRURICount < imageHRUris.size(); imgHRURICount++)
            {
            
             sLogger.debug("inside the hr image selected "+imageHRUris.get(imgHRURICount)+
             " and tn selected "+imageTNUris.get(imgHRURICount));
             selectedImages.add(new Image(Uri.parse(imageHRUris.get(imgHRURICount)), Uri.parse(imageTNUris.get(imgHRURICount))));
            }
            return selectedImages;
        }
        else if(m_imageType.equals(ImageType.REMOTE))
        {
            String textUrl1 = ((EditText) findViewById(R.id.editRemoteImageUrl1)).getText().toString();
            String textUrl2 = ((EditText) findViewById(R.id.editRemoteImageUrl2)).getText().toString();
            if(textUrl1.trim().length() == 0 && textUrl2.trim().length() == 0) {
                throw new RuntimeException("Please fill Remote image URL's");
            }else if(textUrl1.trim().length() > 0)
            {
// downloadRemoteImage(selectedImages, textUrl1);
             selectedImages.add(new Image(Uri.parse(textUrl1), Uri.parse(textUrl1)));
            }else if(textUrl2.trim().length() > 0)
            {
// downloadRemoteImage(selectedImages, textUrl1);
             selectedImages.add(new Image(Uri.parse(textUrl2), Uri.parse(textUrl2)));
            }
        }
        else if(m_imageType.equals(ImageType.SNAPFISH_ALBUM))
        {
            String textUrl1 = ((EditText) findViewById(R.id.editAlbumOid)).getText().toString();
            if(textUrl1.isEmpty())
            {
                throw new RuntimeException("Please enter valied Snapfish account AlbumOid");
            }
            else
            {
                String accountOid = CAccountManager.getAccountOid(getApplicationContext());
                System.out.println("The accountOid is "+accountOid);
                accountOid = accountOid.split("-")[1];
                List<MediaItem> mediaItems = CAccountManager.getAlbumMediaItems(textUrl1+"_"+accountOid);
                for (MediaItem mediaItem : mediaItems) {
                    selectedImages.add(new Image(mediaItem.getAlbumId(), mediaItem.getId(),
                    Uri.parse(mediaItem.getThumbnailUrl().toString())));
                }
            }
            
        }
        return selectedImages;
    }

private void downloadRemoteImage(List<Image> selectedImages, String textUrl1) throws Exception{
URL url = new URL(textUrl1);
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setDoInput(true);
conn.connect();
String downloadedFileName = "download_"+System.currentTimeMillis()+".jpg";
FileOutputStream fos=openFileOutput(downloadedFileName, MODE_WORLD_READABLE);
OutputStreamWriter osw = new OutputStreamWriter(fos);
int b;

InputStream is = conn.getInputStream();
while((b=is.read())!=-1)
{
osw.write(b);
}
is.close();
osw.close();

Bitmap bmImg = BitmapFactory.decodeStream(openFileInput(downloadedFileName));
Bitmap hrImage = ThumbnailUtils.extractThumbnail(bmImg, 400, 600);
Bitmap tnImage = ThumbnailUtils.extractThumbnail(bmImg, 90, 60);


selectedImages.add(new Image(Uri.parse(downloadedFileName), Uri.parse(downloadedFileName)));
}

    private void setDefaultProduct() {
        RadioGroup prdRadioGroup = updatingProductSectionProperties();
        prdRadioGroup.getChildAt(0).setClickable(true);
        int defaultRadioButtonId = prdRadioGroup.getCheckedRadioButtonId();
        selectAProduct(prdRadioGroup, defaultRadioButtonId);
        prdRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
                selectAProduct(rGroup, checkedId);
            }
        });
    }
    
    private void selectAProduct(RadioGroup rGroup, int checkedId) {
        String[] projecSize = getResources().getStringArray(R.array.project_sizes);
        String[] projectMrchOid = getResources().getStringArray(R.array.project_types);
     // This will get the radiobutton that has changed in its check state
        RadioButton checkedRadioButton = (RadioButton)rGroup.findViewById(checkedId);
        // This puts the value (true/false) into the variable
        boolean isChecked = checkedRadioButton.isChecked();
        // If the radiobutton that has changed in check state is now checked...
        if (isChecked)
        {
            
            String text = checkedRadioButton.getText().toString();
            System.out.println("The selected productRadio button is "+text+" projectSize "+projecSize[0]+"---"+projecSize[1]+"---"+projecSize[2]+"---"+projecSize[3]);
            if(text.equals("4x6"))
            {
                m_product = projectMrchOid[1];
            }
            else if(text.equals("5x7"))
            {
                m_product = projectMrchOid[2];
            }
            else if(text.equals("4x4"))
            {
                m_product = projectMrchOid[3];
            }
            else
            {
                m_product="";
            }
// TextView productTextView = (TextView)rGroup.findViewById(R.id.textProductsSelected);
// productTextView.setText(m_product);
            System.out.println("For "+text+" change The value of m_product is"+m_product);
        }
    }
    
    private void selectedEnv(RadioGroup rGroup, int checkedId) {
        // This will get the radiobutton that has changed in its check state
        RadioButton checkedRadioButton = (RadioButton)rGroup.findViewById(checkedId);
        // This puts the value (true/false) into the variable
        boolean isChecked = checkedRadioButton.isChecked();
        // If the radiobutton that has changed in check state is now checked...
        if (isChecked)
        {
            String[] envNames = getResources().getStringArray(R.array.env_array);
            
            String text = checkedRadioButton.getText().toString();
            System.out.println("The selected envRadio button is "+text);
            if(text.equals("int1"))
            {
                String[] envParams = getResources().getStringArray(R.array.int1);
                m_urlEndpoint = envParams[0];
                m_key = envParams[1];
                m_secret = envParams[2];
            }
            else if(text.equals("stg8"))
            {
                String[] envParams = getResources().getStringArray(R.array.stg8);
                m_urlEndpoint = envParams[0];
                m_key = envParams[1];
                m_secret = envParams[2];
            }
            else if(text.equals("prd"))
            {
                String[] envParams = getResources().getStringArray(R.array.prd);
                m_urlEndpoint = envParams[0];
                m_key = envParams[1];
                m_secret = envParams[2];
            }
            else if(text.equals("mir1"))
            {
                String[] envParams = getResources().getStringArray(R.array.mir1);
                m_urlEndpoint = envParams[0];
                m_key = envParams[1];
                m_secret = envParams[2];
            }
            else
            {
                m_urlEndpoint = "";
                m_key= "";
                m_secret = "";
            }
            envSelected = text;
            setBtnReturnedUserEnableOrDisable();
            System.out.println("for env"+text+" change The value of m_urlEndpoint is"+m_urlEndpoint);
        }
        
    }

    private void setDefaultEnvDetails() {
        RadioGroup envRadioGroup = updatingEnvSectionProperties();
        envRadioGroup.getChildAt(0).setClickable(true);
        int defaultRadioButtonId = envRadioGroup.getCheckedRadioButtonId();
        selectedEnv(envRadioGroup, defaultRadioButtonId);
        envRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
                selectedEnv(rGroup, checkedId);
            }
        });
    }
    
    public static final String TARGET_ENV_PARAMS = "envParams";
    private void addOnClickLocalImagesButton() {
        final Button btnLocalImages = (Button)findViewById(R.id.btnLocalImages);
        btnLocalImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_imageType = Image.ImageType.LOCAL;
                View layoutRemoteImages = (View)findViewById(R.id.layoutRemoteImageUrl);
                layoutRemoteImages.setVisibility(View.INVISIBLE);
                View layoutSFAlbums = (View)findViewById(R.id.layoutSnapfishImageAlbum);
                layoutSFAlbums.setVisibility(View.INVISIBLE);
                Intent intent = new Intent(getApplicationContext(), CPhotoGallery.class);
                // intent.putExtra(TARGET_ENV_PARAMS, new String[]{url, key, secret});
                // startActivity(intent);
                startActivityForResult(intent, 1);
            }
        });
        
    }

    private void addOnClickSnapfishImagesButton() {
        final Button btnSnapfishImages = (Button)findViewById(R.id.btnSnapfishImages);
        btnSnapfishImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                m_imageType = Image.ImageType.SNAPFISH_ALBUM;
                View layoutRemoteImages = (View)findViewById(R.id.layoutRemoteImageUrl);
                layoutRemoteImages.setVisibility(View.INVISIBLE);
                View layoutSFAlbums = (View)findViewById(R.id.layoutSnapfishImageAlbum);
                layoutSFAlbums.setVisibility(View.VISIBLE);
            }
        });
        
    }

    private void addOnClickRemoteImagesButton() {
        final Button btnRemoteImages = (Button)findViewById(R.id.btnRemoteImages);
        btnRemoteImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                m_imageType = Image.ImageType.REMOTE;
                View layoutSFAlbums = (View)findViewById(R.id.layoutSnapfishImageAlbum);
                layoutSFAlbums.setVisibility(View.INVISIBLE);
                View layoutRemoteImages = (View)findViewById(R.id.layoutRemoteImageUrl);
                layoutRemoteImages.setVisibility(View.VISIBLE);
            }
        });
    }

    private RadioGroup updateGuestLoginSectionProperties() {
        String[] aryGuestOptions = getResources().getStringArray(R.array.guestOptions);
        RadioGroup guestRadioGroup = (RadioGroup) findViewById(R.id.guestRadioGroup);
        addDynamicButton(aryGuestOptions, guestRadioGroup);
        guestRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
                selectedGuestRadioButtons(rGroup, checkedId);
            }
        });
        
        return guestRadioGroup;
    }

    private void selectedGuestRadioButtons(RadioGroup rGroup, int checkedId) {
        RadioButton checkedRadioButton = (RadioButton)rGroup.findViewById(checkedId);
        // This puts the value (true/false) into the variable
        boolean isChecked = checkedRadioButton.isChecked();
        // If the radiobutton that has changed in check state is now checked...
        if (isChecked)
        {
            String[] guestOptions = getResources().getStringArray(R.array.guestOptions);
            
            String text = checkedRadioButton.getText().toString();
            System.out.println("The selected envRadio button is "+text);
            if(text.equals("email only"))
            {
                userData = new UserData();
                userData.setEmailAddress("jd18@sn.com");
            }
            else
            {
                userData = null;
            }
        }
    }

    private void addOnclickListnerForGuestButton() {
        final Button btnGuestLogin = (Button)findViewById(R.id.btnGuest);
        btnGuestLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                btnGuestLogin.setSelected(true);
                
                sLogger.debug("inside onclick Guest Login button");
                View sfLoginView = (View)findViewById(R.id.sfLoginHiddenLayout);
                sfLoginView.setVisibility(View.INVISIBLE);
                View sfGuestView = (View)findViewById(R.id.sfGuestHiddenLayout);
                sfGuestView.setVisibility(View.INVISIBLE);
                ((Button)findViewById(R.id.btnLocalImages)).setEnabled(true);
                ((Button)findViewById(R.id.btnSnapfishImages)).setEnabled(false);
                ((Button)findViewById(R.id.btnRemoteImages)).setEnabled(true);
                m_loginType = enumLoginType.GUEST;
            }
        });
    }

    private void addOnclickListnerForReturningButton() {
        final Button btnReturningLogin = (Button)findViewById(R.id.btnReturnUser);
        btnReturningLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                btnReturningLogin.setSelected(true);
                sLogger.debug("inside onclick Returned Login button");
                View sfGuestView = (View)findViewById(R.id.sfGuestHiddenLayout);
                sfGuestView.setVisibility(View.INVISIBLE);
                View sfLoginView = (View)findViewById(R.id.sfLoginHiddenLayout);
                sfLoginView.setVisibility(View.INVISIBLE);
                ((Button)findViewById(R.id.btnLocalImages)).setEnabled(true);
                ((Button)findViewById(R.id.btnSnapfishImages)).setEnabled(true);
                ((Button)findViewById(R.id.btnRemoteImages)).setEnabled(true);
                m_loginType = enumLoginType.RETURNING;
            }
        });
        setBtnReturnedUserEnableOrDisable(btnReturningLogin);
    }

    private void setBtnReturnedUserEnableOrDisable(final Button btnReturningLogin) {
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String refreshToken = pref.getString(SnapfishOauth.PERSISTED_OAUTH_DATA, null);
        String envLastTimeExecuted = pref.getString(ENV_LAST_TIME_EXECUTED, null);
        sLogger.debug("The value of refreshToken is the testapp is |"+refreshToken);
        if(refreshToken == null || ( envLastTimeExecuted != null && !envSelected.equals(envLastTimeExecuted)))
        {
            btnReturningLogin.setEnabled(false);
        }
        else {
            btnReturningLogin.setEnabled(true);
        }
    }
    
    private void setBtnReturnedUserEnableOrDisable() {
        final Button btnReturningLogin = (Button)findViewById(R.id.btnReturnUser);
        setBtnReturnedUserEnableOrDisable(btnReturningLogin);
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
        sLogger.debug("\n\n\n\n on resume");
        setBtnReturnedUserEnableOrDisable();
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        setBtnReturnedUserEnableOrDisable();
    }

    private void addOnclickListnerForSnapfishLoginButton() {
        final Button btnSnapfishLogin = (Button)findViewById(R.id.btnSnapfishLogin);
        btnSnapfishLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sLogger.debug("inside onclick Snapfish Login button");
                defaultLoginType();
            }
        });
    }
    
    private void defaultLoginType() {
        View sfGuestView = (View)findViewById(R.id.sfGuestHiddenLayout);
        sfGuestView.setVisibility(View.INVISIBLE);
        View sfLoginView = (View)findViewById(R.id.sfLoginHiddenLayout);
        sfLoginView.setVisibility(View.VISIBLE);
        ((Button)findViewById(R.id.btnLocalImages)).setEnabled(true);
        ((Button)findViewById(R.id.btnSnapfishImages)).setEnabled(true);
        ((Button)findViewById(R.id.btnRemoteImages)).setEnabled(true);
        ((EditText) findViewById(R.id.editUserId)).setText("jd18@sn.com");
        ((EditText) findViewById(R.id.editPasswd)).setText("adgjm");
        m_loginType = enumLoginType.SNAFISH;
    }

    private RadioGroup updatingProductSectionProperties() {
        String[] aryProductNames = getResources().getStringArray(R.array.project_sizes);
        TextView envtextView = (TextView) findViewById(R.id.textProducts);
        envtextView.setText(aryProductNames[0]);
        RadioGroup productRadioGroup = (RadioGroup) findViewById(R.id.productRadioGroup);
        addDynamicButton(aryProductNames, productRadioGroup);
        return productRadioGroup;
    }

    private RadioGroup updatingEnvSectionProperties() {
        String[] aryEvnNames = getResources().getStringArray(R.array.env_array);
        TextView envtextView = (TextView) findViewById(R.id.textEnvironments);
        envtextView.setText(aryEvnNames[0]);
        RadioGroup envRadioGroup = (RadioGroup) findViewById(R.id.envRadioGroup);
        addDynamicButton(aryEvnNames, envRadioGroup);
        return envRadioGroup;
    }

    private void addDynamicButton(String[] aryEvnNames, RadioGroup envRadioGroup) {
        int chldCount = envRadioGroup.getChildCount();
        
        RadioButton tmpStaticButton = null;
// if(aryEvnNames.length > 1 && aryEvnNames.length <= (chldCount-1))
// {
// System.out.println("inside less arry size ");
// for (int i = aryEvnNames.length; i < (chldCount); i++) {
// envRadioGroup.removeViewAt(i);
// }
// }
// chldCount = envRadioGroup.getChildCount();
// System.out.println(" After ary envNAmeLenght "+aryEvnNames.length+" and linerLayoutCouint -1 is "+(chldCount-1));
        for (int i = 0; i < chldCount; i++) {
            RadioButton button = (RadioButton) envRadioGroup.getChildAt(i);
            if (i == 0) {
// button.setChecked(true);
                tmpStaticButton = button;
            }
            button.setText(aryEvnNames[i + 1]);
        }
        
        if(tmpStaticButton != null && aryEvnNames.length > (chldCount+1))
        {
            for (int i = (chldCount+1); i < aryEvnNames.length; i++) {
                System.out.println("The value of ary element for i "+(i)+"is "+aryEvnNames[i]);
                if(aryEvnNames[i].equalsIgnoreCase("custom") ||
                        aryEvnNames[i].equalsIgnoreCase("placeholder")) {
                    continue;
                }
                
                RadioButton button = new RadioButton(this);
                button.setText(aryEvnNames[i]);
                button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                button.setTextSize(10);
                envRadioGroup.addView(button);
            }
            
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.test_app, menu);
        return true;
    }
    
    enum enumLoginType {SNAFISH, RETURNING, GUEST};
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                imageHRUris = data.getStringArrayListExtra(CPhotoGallery.EXTRAS_HR_PATH);
                imageTNUris = data.getStringArrayListExtra(CPhotoGallery.EXTRAS_TN_PATH);

                for(String uri : imageHRUris)
                    System.out.println(" activity result image URI: " + uri);
            }
        }

    }
    


}
