package com.laleme.app.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 界面级测试：专门盯住「添加按钮是否真的显示在屏幕上、且可以点击」。
 *
 * 之前踩过的两个坑都在这里被覆盖：
 *  1. 反馈提示条盖住了右上角的 ＋ —— 用 assertIsDisplayed + 实际点击来验证；
 *  2. 顶部栏被「总结」按钮撑满、把 ＋ 挤出屏幕 —— 用 viewport 边界来验证。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class AddButtonUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val addDesc = "添加拉屎记录"

    @Test
    fun `空状态下添加按钮可见且可点击`() {
        rule.setContent { AppRoot() }
        rule.waitForIdle()

        rule.onNodeWithContentDescription(addDesc)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun `总结按钮不会被撑满整宽_添加按钮依然在屏幕内`() {
        rule.setContent { AppRoot() }
        rule.waitForIdle()

        val root = rule.onRoot().fetchSemanticsNode()
        val rootRight = root.boundsInRoot.right

        val summary = rule.onNodeWithText("总结").fetchSemanticsNode()
        val add = rule.onNodeWithContentDescription(addDesc).fetchSemanticsNode()

        // 「总结」按钮不应该横跨整屏，否则标题和 ＋ 会被挤出去
        assertTrue(
            "「总结」按钮宽度 ${summary.boundsInRoot.width} 过大（屏幕宽 $rootRight），会把 ＋ 挤出屏幕",
            summary.boundsInRoot.width < rootRight * 0.5f
        )

        // ＋ 必须完全落在屏幕范围内
        assertTrue(
            "添加按钮跑到屏幕外了：left=${add.boundsInRoot.left} right=${add.boundsInRoot.right} 屏幕宽=$rootRight",
            add.boundsInRoot.left >= 0f && add.boundsInRoot.right <= rootRight
        )

        rule.onNodeWithContentDescription(addDesc).assertIsDisplayed()
    }

    @Test
    fun `添加一条之后_添加按钮仍然可见可点击`() {
        rule.setContent { AppRoot() }
        rule.waitForIdle()

        // 打开面板并保存
        rule.onNodeWithContentDescription(addDesc).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("记下了").assertIsDisplayed().performClick()
        rule.waitForIdle()

        // 关键断言：保存之后 ＋ 依然要在屏幕里、并且能点
        rule.onNodeWithContentDescription(addDesc)
            .assertIsDisplayed()
            .assertHasClickAction()

        // 而且要真的还能再打开一次面板
        rule.onNodeWithContentDescription(addDesc).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("记下了").assertIsDisplayed()
    }

    @Test
    fun `反馈提示条不会遮挡添加按钮`() {
        rule.setContent { AppRoot() }
        rule.waitForIdle()

        rule.onNodeWithContentDescription(addDesc).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("记下了").performClick()
        rule.waitForIdle()

        // 提示条出现后，添加按钮的点击区域不应该被它覆盖：
        // 用一次真实点击来验证——如果被盖住，点击不会有任何反应。
        rule.onNodeWithContentDescription(addDesc).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("记下了")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
