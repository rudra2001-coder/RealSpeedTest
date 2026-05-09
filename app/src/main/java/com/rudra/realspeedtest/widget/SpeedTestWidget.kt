package com.rudra.realspeedtest.widget

import android.R.attr.fontWeight
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rudra.realspeedtest.MainActivity
import com.rudra.realspeedtest.R
//
//class SpeedTestWidget : GlanceAppWidget() {

//    override suspend fun provideGlance(context: Context, id: GlanceId) {
//        provideContent {
//            GlanceTheme {
//                WidgetContent()
//            }
//        }
//    }
////
//    @Composable
//    private fun WidgetContent() {
//        Box(
//            modifier = GlanceModifier
//                .fillMaxSize()
//                .background(Color(0xFF388E3C))
//                .cornerRadius(16.dp)
//                .clickable(actionStartActivity<MainActivity>())
//                .padding(16.dp)
//        ) {
//            Column(
//                modifier = GlanceModifier.fillMaxSize(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(
//                    modifier = GlanceModifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Real Speed Test",
//                        style = TextStyle(
//                            color = (#5655545)
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//                    )
//                }
//                Spacer(modifier = GlanceModifier.height(8.dp))
//                Row(
//                    modifier = GlanceModifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Tap to run test",
//                        style = TextStyle(
//                            color = White.copy(alpha = 0.8f),
//                            fontSize = 12.sp
//                        )
//                    )
//                }
//                Spacer(modifier = GlanceModifier.height(12.dp))
//                Row(
//                    modifier = GlanceModifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(
//                        modifier = GlanceModifier
//                            .size(48.dp)
//                            .background(White.copy(alpha = 0.2f))
//                            .cornerRadius(24.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "▶",
//                            style = TextStyle(
//                                color = White,
//                                fontSize = 20.sp
//                            )
//                        )
//                    }
//                    Spacer(modifier = GlanceModifier.width(12.dp))
//                    Column {
//                        Text(
//                            text = "Start Test",
//                            style = TextStyle(
//                                color = White,
//                                fontSize = 14.sp,
//                                fontWeight = FontWeight.Medium
//                            )
//                        )
//                        Text(
//                            text = "Check your internet speed",
//                            style = TextStyle(
//                                color = White.copy(alpha = 0.7f),
//                                fontSize = 10.sp
//                            )
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//
//}
//
//class SpeedTestWidgetReceiver : GlanceAppWidgetReceiver() {
//    override val glanceAppWidget: GlanceAppWidget = SpeedTestWidget()
//}