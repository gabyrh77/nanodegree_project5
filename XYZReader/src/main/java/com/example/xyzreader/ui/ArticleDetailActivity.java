package com.example.xyzreader.ui;


import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final String ID_TAG = "selected_id_tag";
    private Cursor mCursor;

    private long mSelectedItemId;
    private FloatingActionButton mShareButton;
    private TextView mTitleView;
    private ImageView mPhotoView;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mSelectedItemId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }else{
            mSelectedItemId = savedInstanceState.getLong(ID_TAG);
        }

        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_TAG, mSelectedItemId);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(this, mSelectedItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        bindViews();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
          if(mCursor!=null && !mCursor.isClosed()){
            mCursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }

    private void bindViews() {

        mTitleView = (TextView) findViewById(R.id.article_title);
        TextView bylineView = (TextView) findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) findViewById(R.id.article_body);
        View metaBar = findViewById(R.id.meta_bar);
        final int barSize = metaBar.getHeight();
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        mShareButton = (FloatingActionButton) findViewById(R.id.share_fab);
        mPhotoView = (ImageView) findViewById(R.id.photo);
        if (mCursor != null && mCursor.moveToFirst()) {
//            mRootView.setAlpha(0);
//            mRootView.setVisibility(View.VISIBLE);
//            mRootView.animate().alpha(1);
            final String title = mCursor.getString(ArticleLoader.Query.TITLE);
            final String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            mTitleView.setText(title);
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + author
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            Glide.with(this).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL)).into(mPhotoView);

            mShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                            .setType("text/plain")
                            .setText(getString(R.string.text_share, title, author))
                            .getIntent(), getString(R.string.action_share)));
                }
            });


            mTitleView.post(new Runnable() {
                @Override
                public void run() {
                    int newBarSize = 0;
                    CollapsingToolbarLayout.LayoutParams layoutParams = (CollapsingToolbarLayout.LayoutParams) mToolbar.getLayoutParams();
                    if (barSize> mToolbar.getHeight()){
                        newBarSize = barSize;
                    }else{
                        newBarSize = mToolbar.getHeight();
                    }
                    layoutParams.height = newBarSize;
                    int lines = mTitleView.getLineCount();
                    if(lines>1){
                        layoutParams.height +=  (( layoutParams.height/2)*(lines-1));
                    }
                    mToolbar.setLayoutParams(layoutParams);
                    mToolbar.requestLayout();
                }
            });


        } else {
            mTitleView.setText("");
            bylineView.setText("");
            bodyView.setText("");
        }
    }
}
