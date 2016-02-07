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
import android.util.Log;
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
    private static final String LOG_TAG = ArticleDetailActivity.class.getSimpleName();
    private long mSelectedItemId;
    private FloatingActionButton mShareButton;
    private TextView mTitleView;
    private TextView mByLineView;
    private TextView mBodyView;
    private ImageView mPhotoView;
    private Toolbar mToolbar;
    private View mScrollView;
    private View mBarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mScrollView = findViewById(R.id.scroll_view);
        mTitleView = (TextView) findViewById(R.id.article_title);
        mByLineView = (TextView) findViewById(R.id.article_byline);
        mByLineView.setMovementMethod(new LinkMovementMethod());
        mBodyView = (TextView) findViewById(R.id.article_body);
        mBarView = findViewById(R.id.meta_bar);
        mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        mShareButton = (FloatingActionButton) findViewById(R.id.share_fab);
        mPhotoView = (ImageView) findViewById(R.id.photo);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mSelectedItemId = ItemsContract.Items.getItemId(getIntent().getData());
            }else{
                Log.e(LOG_TAG, "Error, not receiving selected item id");
            }
        }else{
            mSelectedItemId = savedInstanceState.getLong(ID_TAG);
        }

        getLoaderManager().initLoader(0, null, this);
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
        bindViews(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    private void bindViews(Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            final String title = cursor.getString(ArticleLoader.Query.TITLE);
            final String author = cursor.getString(ArticleLoader.Query.AUTHOR);
            mTitleView.setText(title);
            mByLineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            cursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + author
                            + "</font>"));
            mBodyView.setText(Html.fromHtml(cursor.getString(ArticleLoader.Query.BODY)));
            Glide.with(this).load(cursor.getString(ArticleLoader.Query.PHOTO_URL)).into(mPhotoView);

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
                    final int newbarSize = mBarView.getHeight();
                    CollapsingToolbarLayout.LayoutParams layoutParams = (CollapsingToolbarLayout.LayoutParams) mToolbar.getLayoutParams();
                    layoutParams.height = newbarSize;
                    mScrollView.setPadding(0, 0, 0, layoutParams.height);
                    mToolbar.setLayoutParams(layoutParams);
                    mToolbar.requestLayout();
                }
            });
        }
    }
}
