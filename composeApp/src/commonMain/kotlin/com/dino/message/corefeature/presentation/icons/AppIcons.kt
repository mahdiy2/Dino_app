package com.dino.message.corefeature.presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.icons.AppIcons

object AppIcons {
    val AccessTime: ImageVector
        get() {
            if (_accessTime != null) return _accessTime!!
            _accessTime = appIcon(name = "AppIcons.AccessTime") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                    moveTo(12f,7f)
                    verticalLineTo(12f)
                    lineTo(16f,14f)
                }
            }
            return _accessTime!!
        }

    val Add: ImageVector
        get() {
            if (_add != null) return _add!!
            _add = appIcon(name = "AppIcons.Add") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,5f)
                    verticalLineTo(19f)
                    moveTo(5f,12f)
                    horizontalLineTo(19f)
                }
            }
            return _add!!
        }

    val AlternateEmail: ImageVector
        get() {
            if (_alternateEmail != null) return _alternateEmail!!
            _alternateEmail = appIcon(name = "AppIcons.AlternateEmail") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,4f)
                    lineTo(19f,8f)
                    verticalLineTo(16f)
                    lineTo(12f,20f)
                    lineTo(5f,16f)
                    verticalLineTo(8f)
                    lineTo(12f,4f)
                    moveTo(9f,12f)
                    lineTo(12f,9f)
                    lineTo(15f,12f)
                    lineTo(12f,15f)
                    lineTo(9f,12f)
                }
            }
            return _alternateEmail!!
        }

    val ArrowBackIosNew: ImageVector
        get() {
            if (_arrowBackIosNew != null) return _arrowBackIosNew!!
            _arrowBackIosNew = appIcon(name = "AppIcons.ArrowBackIosNew") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(15f,5f)
                    lineTo(8f,12f)
                    lineTo(15f,19f)
                }
            }
            return _arrowBackIosNew!!
        }

    val AttachFile: ImageVector
        get() {
            if (_attachFile != null) return _attachFile!!
            _attachFile = appIcon(name = "AppIcons.AttachFile") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(8f,12f)
                    lineTo(14f,6f)
                    lineTo(18f,10f)
                    lineTo(10f,18f)
                    lineTo(6f,14f)
                    lineTo(14f,6f)
                }
            }
            return _attachFile!!
        }

    val BarChart: ImageVector
        get() {
            if (_barChart != null) return _barChart!!
            _barChart = appIcon(name = "AppIcons.BarChart") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,20f)
                    verticalLineTo(12f)
                    moveTo(12f,20f)
                    verticalLineTo(6f)
                    moveTo(18f,20f)
                    verticalLineTo(10f)
                }
            }
            return _barChart!!
        }

    val Block: ImageVector
        get() {
            if (_block != null) return _block!!
            _block = appIcon(name = "AppIcons.Block") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,5f)
                    lineTo(19f,19f)
                    moveTo(19f,5f)
                    lineTo(5f,19f)
                }
            }
            return _block!!
        }

    val BrokenImage: ImageVector
        get() {
            if (_brokenImage != null) return _brokenImage!!
            _brokenImage = appIcon(name = "AppIcons.BrokenImage") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,6f)
                    horizontalLineTo(20f)
                    verticalLineTo(18f)
                    horizontalLineTo(4f)
                    verticalLineTo(6f)
                    moveTo(6f,16f)
                    lineTo(10f,12f)
                    lineTo(13f,15f)
                    lineTo(15f,13f)
                    lineTo(19f,17f)
                    moveTo(16f,9f)
                    lineTo(16.01f,9f)
                }
            }
            return _brokenImage!!
        }

    val Brush: ImageVector
        get() {
            if (_brush != null) return _brush!!
            _brush = appIcon(name = "AppIcons.Brush") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,18f)
                    lineTo(9f,21f)
                    lineTo(12f,18f)
                    moveTo(12f,18f)
                    lineTo(20f,6f)
                    lineTo(18f,4f)
                    lineTo(10f,16f)
                }
            }
            return _brush!!
        }

    val BugReport: ImageVector
        get() {
            if (_bugReport != null) return _bugReport!!
            _bugReport = appIcon(name = "AppIcons.BugReport") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(7f,8f)
                    horizontalLineTo(17f)
                    verticalLineTo(18f)
                    horizontalLineTo(7f)
                    verticalLineTo(8f)
                    moveTo(9f,5f)
                    lineTo(15f,5f)
                    moveTo(4f,11f)
                    horizontalLineTo(7f)
                    moveTo(17f,11f)
                    horizontalLineTo(20f)
                    moveTo(4f,15f)
                    horizontalLineTo(7f)
                    moveTo(17f,15f)
                    horizontalLineTo(20f)
                }
            }
            return _bugReport!!
        }

    val CameraAlt: ImageVector
        get() {
            if (_cameraAlt != null) return _cameraAlt!!
            _cameraAlt = appIcon(name = "AppIcons.CameraAlt") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,8f)
                    horizontalLineTo(20f)
                    verticalLineTo(19f)
                    horizontalLineTo(4f)
                    verticalLineTo(8f)
                    moveTo(9f,8f)
                    lineTo(10f,5f)
                    horizontalLineTo(14f)
                    lineTo(15f,8f)
                    moveTo(9f,14f)
                    lineTo(12f,11f)
                    lineTo(15f,14f)
                    lineTo(12f,17f)
                    lineTo(9f,14f)
                }
            }
            return _cameraAlt!!
        }

    val Check: ImageVector
        get() {
            if (_check != null) return _check!!
            _check = appIcon(name = "AppIcons.Check") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,12.5f)
                    lineTo(10f,17f)
                    lineTo(19f,7f)
                }
            }
            return _check!!
        }

    val CheckCircle: ImageVector
        get() {
            if (_checkCircle != null) return _checkCircle!!
            _checkCircle = appIcon(name = "AppIcons.CheckCircle") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,12f)
                    lineTo(10f,17f)
                    lineTo(19f,7f)
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                }
            }
            return _checkCircle!!
        }

    val Clear: ImageVector
        get() {
            if (_clear != null) return _clear!!
            _clear = appIcon(name = "AppIcons.Clear") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,6f)
                    lineTo(18f,18f)
                    moveTo(18f,6f)
                    lineTo(6f,18f)
                }
            }
            return _clear!!
        }

    val Close: ImageVector
        get() {
            if (_close != null) return _close!!
            _close = appIcon(name = "AppIcons.Close") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,6f)
                    lineTo(18f,18f)
                    moveTo(18f,6f)
                    lineTo(6f,18f)
                }
            }
            return _close!!
        }

    val ContentCopy: ImageVector
        get() {
            if (_contentCopy != null) return _contentCopy!!
            _contentCopy = appIcon(name = "AppIcons.ContentCopy") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(8f,8f)
                    horizontalLineTo(18f)
                    verticalLineTo(20f)
                    horizontalLineTo(8f)
                    verticalLineTo(8f)
                    moveTo(6f,4f)
                    horizontalLineTo(16f)
                    verticalLineTo(6f)
                }
            }
            return _contentCopy!!
        }

    val Create: ImageVector
        get() {
            if (_create != null) return _create!!
            _create = appIcon(name = "AppIcons.Create") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,19f)
                    lineTo(8f,14f)
                    lineTo(17f,5f)
                    lineTo(20f,8f)
                    lineTo(11f,17f)
                    lineTo(5f,19f)
                }
            }
            return _create!!
        }

    val Crop: ImageVector
        get() {
            if (_crop != null) return _crop!!
            _crop = appIcon(name = "AppIcons.Crop") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,3f)
                    verticalLineTo(18f)
                    horizontalLineTo(21f)
                    moveTo(3f,6f)
                    horizontalLineTo(18f)
                    verticalLineTo(21f)
                }
            }
            return _crop!!
        }

    val Delete: ImageVector
        get() {
            if (_delete != null) return _delete!!
            _delete = appIcon(name = "AppIcons.Delete") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(7f,8f)
                    verticalLineTo(20f)
                    horizontalLineTo(17f)
                    verticalLineTo(8f)
                    moveTo(5f,6f)
                    horizontalLineTo(19f)
                    moveTo(9f,4f)
                    horizontalLineTo(15f)
                }
            }
            return _delete!!
        }

    val DeleteForever: ImageVector
        get() {
            if (_deleteForever != null) return _deleteForever!!
            _deleteForever = appIcon(name = "AppIcons.DeleteForever") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(7f,8f)
                    verticalLineTo(20f)
                    horizontalLineTo(17f)
                    verticalLineTo(8f)
                    moveTo(5f,6f)
                    horizontalLineTo(19f)
                    moveTo(9f,11f)
                    lineTo(15f,17f)
                    moveTo(15f,11f)
                    lineTo(9f,17f)
                }
            }
            return _deleteForever!!
        }

    val Done: ImageVector
        get() {
            if (_done != null) return _done!!
            _done = appIcon(name = "AppIcons.Done") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,10f)
                    verticalLineTo(17f)
                    moveTo(12f,7f)
                    lineTo(12.01f,7f)
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                }
            }
            return _done!!
        }

    val DoneAll: ImageVector
        get() {
            if (_doneAll != null) return _doneAll!!
            _doneAll = appIcon(name = "AppIcons.DoneAll") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,10f)
                    verticalLineTo(17f)
                    moveTo(12f,7f)
                    lineTo(12.01f,7f)
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                }
            }
            return _doneAll!!
        }

    val Download: ImageVector
        get() {
            if (_download != null) return _download!!
            _download = appIcon(name = "AppIcons.Download") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,4f)
                    verticalLineTo(15f)
                    moveTo(8f,11f)
                    lineTo(12f,15f)
                    lineTo(16f,11f)
                    moveTo(5f,19f)
                    horizontalLineTo(19f)
                }
            }
            return _download!!
        }

    val Edit: ImageVector
        get() {
            if (_edit != null) return _edit!!
            _edit = appIcon(name = "AppIcons.Edit") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,19f)
                    lineTo(8f,14f)
                    lineTo(17f,5f)
                    lineTo(20f,8f)
                    lineTo(11f,17f)
                    lineTo(5f,19f)
                }
            }
            return _edit!!
        }

    val ErrorOutline: ImageVector
        get() {
            if (_errorOutline != null) return _errorOutline!!
            _errorOutline = appIcon(name = "AppIcons.ErrorOutline") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,10f)
                    verticalLineTo(17f)
                    moveTo(12f,7f)
                    lineTo(12.01f,7f)
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                }
            }
            return _errorOutline!!
        }

    val ExitToApp: ImageVector
        get() {
            if (_exitToApp != null) return _exitToApp!!
            _exitToApp = appIcon(name = "AppIcons.ExitToApp") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,5f)
                    horizontalLineTo(14f)
                    verticalLineTo(9f)
                    moveTo(14f,15f)
                    verticalLineTo(19f)
                    horizontalLineTo(5f)
                    verticalLineTo(5f)
                    moveTo(11f,12f)
                    horizontalLineTo(21f)
                    moveTo(17f,8f)
                    lineTo(21f,12f)
                    lineTo(17f,16f)
                }
            }
            return _exitToApp!!
        }

    val Favorite: ImageVector
        get() {
            if (_favorite != null) return _favorite!!
            _favorite = appIcon(name = "AppIcons.Favorite") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,20f)
                    lineTo(6f,14f)
                    lineTo(5f,9f)
                    lineTo(8f,6f)
                    lineTo(12f,9f)
                    lineTo(16f,6f)
                    lineTo(19f,9f)
                    lineTo(18f,14f)
                    lineTo(12f,20f)
                }
            }
            return _favorite!!
        }

    val FavoriteBorder: ImageVector
        get() {
            if (_favoriteBorder != null) return _favoriteBorder!!
            _favoriteBorder = appIcon(name = "AppIcons.FavoriteBorder") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,20f)
                    lineTo(6f,14f)
                    lineTo(5f,9f)
                    lineTo(8f,6f)
                    lineTo(12f,9f)
                    lineTo(16f,6f)
                    lineTo(19f,9f)
                    lineTo(18f,14f)
                    lineTo(12f,20f)
                }
            }
            return _favoriteBorder!!
        }

    val FormatColorFill: ImageVector
        get() {
            if (_formatColorFill != null) return _formatColorFill!!
            _formatColorFill = appIcon(name = "AppIcons.FormatColorFill") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(7f,13f)
                    lineTo(12f,5f)
                    lineTo(17f,13f)
                    lineTo(12f,18f)
                    lineTo(7f,13f)
                    moveTo(12f,18f)
                    horizontalLineTo(20f)
                }
            }
            return _formatColorFill!!
        }

    val FormatColorText: ImageVector
        get() {
            if (_formatColorText != null) return _formatColorText!!
            _formatColorText = appIcon(name = "AppIcons.FormatColorText") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,20f)
                    horizontalLineTo(19f)
                    moveTo(8f,16f)
                    lineTo(12f,5f)
                    lineTo(16f,16f)
                    moveTo(9f,13f)
                    horizontalLineTo(15f)
                }
            }
            return _formatColorText!!
        }

    val Forum: ImageVector
        get() {
            if (_forum != null) return _forum!!
            _forum = appIcon(name = "AppIcons.Forum") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,10f)
                    verticalLineTo(17f)
                    moveTo(12f,7f)
                    lineTo(12.01f,7f)
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                }
            }
            return _forum!!
        }

    val Group: ImageVector
        get() {
            if (_group != null) return _group!!
            _group = appIcon(name = "AppIcons.Group") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(8f,9f)
                    lineTo(10f,7f)
                    lineTo(12f,9f)
                    lineTo(10f,11f)
                    lineTo(8f,9f)
                    moveTo(14f,9f)
                    lineTo(16f,7f)
                    lineTo(18f,9f)
                    lineTo(16f,11f)
                    lineTo(14f,9f)
                    moveTo(4f,20f)
                    lineTo(7f,15f)
                    horizontalLineTo(17f)
                    lineTo(20f,20f)
                }
            }
            return _group!!
        }

    val Image: ImageVector
        get() {
            if (_image != null) return _image!!
            _image = appIcon(name = "AppIcons.Image") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,6f)
                    horizontalLineTo(20f)
                    verticalLineTo(18f)
                    horizontalLineTo(4f)
                    verticalLineTo(6f)
                    moveTo(6f,16f)
                    lineTo(10f,12f)
                    lineTo(13f,15f)
                    lineTo(15f,13f)
                    lineTo(19f,17f)
                    moveTo(16f,9f)
                    lineTo(16.01f,9f)
                }
            }
            return _image!!
        }

    val Info: ImageVector
        get() {
            if (_info != null) return _info!!
            _info = appIcon(name = "AppIcons.Info") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,10f)
                    verticalLineTo(17f)
                    moveTo(12f,7f)
                    lineTo(12.01f,7f)
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                }
            }
            return _info!!
        }

    val KeyboardArrowRight: ImageVector
        get() {
            if (_keyboardArrowRight != null) return _keyboardArrowRight!!
            _keyboardArrowRight = appIcon(name = "AppIcons.KeyboardArrowRight") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(9f,5f)
                    lineTo(16f,12f)
                    lineTo(9f,19f)
                }
            }
            return _keyboardArrowRight!!
        }

    val Language: ImageVector
        get() {
            if (_language != null) return _language!!
            _language = appIcon(name = "AppIcons.Language") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,3f)
                    lineTo(19f,7f)
                    verticalLineTo(17f)
                    lineTo(12f,21f)
                    lineTo(5f,17f)
                    verticalLineTo(7f)
                    lineTo(12f,3f)
                    moveTo(5f,12f)
                    horizontalLineTo(19f)
                    moveTo(12f,3f)
                    verticalLineTo(21f)
                }
            }
            return _language!!
        }

    val LibraryMusic: ImageVector
        get() {
            if (_libraryMusic != null) return _libraryMusic!!
            _libraryMusic = appIcon(name = "AppIcons.LibraryMusic") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,4f)
                    horizontalLineTo(18f)
                    verticalLineTo(20f)
                    horizontalLineTo(6f)
                    verticalLineTo(4f)
                    moveTo(11f,16f)
                    lineTo(8f,18f)
                    lineTo(6f,16f)
                    lineTo(9f,14f)
                    lineTo(11f,16f)
                    moveTo(11f,16f)
                    verticalLineTo(8f)
                    lineTo(16f,7f)
                }
            }
            return _libraryMusic!!
        }

    val Link: ImageVector
        get() {
            if (_link != null) return _link!!
            _link = appIcon(name = "AppIcons.Link") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(8f,12f)
                    horizontalLineTo(16f)
                    moveTo(9f,8f)
                    lineTo(6f,8f)
                    lineTo(3f,12f)
                    lineTo(6f,16f)
                    lineTo(9f,16f)
                    moveTo(15f,8f)
                    lineTo(18f,8f)
                    lineTo(21f,12f)
                    lineTo(18f,16f)
                    lineTo(15f,16f)
                }
            }
            return _link!!
        }

    val Mic: ImageVector
        get() {
            if (_mic != null) return _mic!!
            _mic = appIcon(name = "AppIcons.Mic") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,4f)
                    lineTo(15f,7f)
                    verticalLineTo(12f)
                    lineTo(12f,15f)
                    lineTo(9f,12f)
                    verticalLineTo(7f)
                    lineTo(12f,4f)
                    moveTo(5f,12f)
                    lineTo(8f,18f)
                    lineTo(12f,19f)
                    lineTo(16f,18f)
                    lineTo(19f,12f)
                    moveTo(12f,19f)
                    verticalLineTo(22f)
                }
            }
            return _mic!!
        }

    val MoreVert: ImageVector
        get() {
            if (_moreVert != null) return _moreVert!!
            _moreVert = appIcon(name = "AppIcons.MoreVert") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,7f)
                    lineTo(12.01f,7f)
                    moveTo(12f,12f)
                    lineTo(12.01f,12f)
                    moveTo(12f,17f)
                    lineTo(12.01f,17f)
                }
            }
            return _moreVert!!
        }

    val Movie: ImageVector
        get() {
            if (_movie != null) return _movie!!
            _movie = appIcon(name = "AppIcons.Movie") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,6f)
                    horizontalLineTo(20f)
                    verticalLineTo(18f)
                    horizontalLineTo(4f)
                    verticalLineTo(6f)
                    moveTo(8f,6f)
                    verticalLineTo(18f)
                    moveTo(16f,6f)
                    verticalLineTo(18f)
                }
            }
            return _movie!!
        }

    val MusicNote: ImageVector
        get() {
            if (_musicNote != null) return _musicNote!!
            _musicNote = appIcon(name = "AppIcons.MusicNote") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(10f,18f)
                    lineTo(7f,20f)
                    lineTo(4f,18f)
                    lineTo(7f,16f)
                    lineTo(10f,18f)
                    moveTo(10f,18f)
                    verticalLineTo(6f)
                    lineTo(18f,4f)
                    verticalLineTo(8f)
                    lineTo(10f,10f)
                }
            }
            return _musicNote!!
        }

    val Pause: ImageVector
        get() {
            if (_pause != null) return _pause!!
            _pause = appIcon(name = "AppIcons.Pause") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(8f,5f)
                    verticalLineTo(19f)
                    moveTo(16f,5f)
                    verticalLineTo(19f)
                }
            }
            return _pause!!
        }

    val Person: ImageVector
        get() {
            if (_person != null) return _person!!
            _person = appIcon(name = "AppIcons.Person") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(9f,8f)
                    lineTo(12f,5f)
                    lineTo(15f,8f)
                    lineTo(12f,11f)
                    lineTo(9f,8f)
                    moveTo(5f,20f)
                    lineTo(8f,15f)
                    horizontalLineTo(16f)
                    lineTo(19f,20f)
                }
            }
            return _person!!
        }

    val PlayArrow: ImageVector
        get() {
            if (_playArrow != null) return _playArrow!!
            _playArrow = appIcon(name = "AppIcons.PlayArrow") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(8f,5f)
                    lineTo(19f,12f)
                    lineTo(8f,19f)
                    lineTo(8f,5f)
                }
            }
            return _playArrow!!
        }

    val Redo: ImageVector
        get() {
            if (_redo != null) return _redo!!
            _redo = appIcon(name = "AppIcons.Redo") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(14f,7f)
                    lineTo(20f,12f)
                    lineTo(14f,17f)
                    moveTo(19f,12f)
                    horizontalLineTo(8f)
                    lineTo(4f,16f)
                }
            }
            return _redo!!
        }

    val Refresh: ImageVector
        get() {
            if (_refresh != null) return _refresh!!
            _refresh = appIcon(name = "AppIcons.Refresh") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(19f,8f)
                    horizontalLineTo(15f)
                    moveTo(19f,8f)
                    verticalLineTo(4f)
                    moveTo(19f,8f)
                    lineTo(16f,6f)
                    lineTo(11f,5f)
                    lineTo(7f,8f)
                    lineTo(5f,12f)
                    moveTo(5f,16f)
                    horizontalLineTo(9f)
                    moveTo(5f,16f)
                    verticalLineTo(20f)
                    moveTo(5f,16f)
                    lineTo(8f,18f)
                    lineTo(13f,19f)
                    lineTo(17f,16f)
                    lineTo(19f,12f)
                }
            }
            return _refresh!!
        }

    val Repeat: ImageVector
        get() {
            if (_repeat != null) return _repeat!!
            _repeat = appIcon(name = "AppIcons.Repeat") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(7f,7f)
                    horizontalLineTo(18f)
                    lineTo(21f,10f)
                    moveTo(21f,10f)
                    lineTo(18f,13f)
                    moveTo(17f,17f)
                    horizontalLineTo(6f)
                    lineTo(3f,14f)
                    moveTo(3f,14f)
                    lineTo(6f,11f)
                }
            }
            return _repeat!!
        }

    val Reply: ImageVector
        get() {
            if (_reply != null) return _reply!!
            _reply = appIcon(name = "AppIcons.Reply") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(10f,7f)
                    lineTo(4f,12f)
                    lineTo(10f,17f)
                    moveTo(5f,12f)
                    horizontalLineTo(16f)
                    lineTo(20f,16f)
                }
            }
            return _reply!!
        }

    val Search: ImageVector
        get() {
            if (_search != null) return _search!!
            _search = appIcon(name = "AppIcons.Search") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,11f)
                    lineTo(7f,6f)
                    lineTo(12f,4f)
                    lineTo(17f,6f)
                    lineTo(19f,11f)
                    lineTo(17f,16f)
                    lineTo(12f,18f)
                    lineTo(7f,16f)
                    lineTo(5f,11f)
                    moveTo(16f,16f)
                    lineTo(21f,21f)
                }
            }
            return _search!!
        }

    val Send: ImageVector
        get() {
            if (_send != null) return _send!!
            _send = appIcon(name = "AppIcons.Send") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,5f)
                    lineTo(20f,12f)
                    lineTo(4f,19f)
                    lineTo(7f,13f)
                    lineTo(14f,12f)
                    lineTo(7f,11f)
                    lineTo(4f,5f)
                }
            }
            return _send!!
        }

    val Settings: ImageVector
        get() {
            if (_settings != null) return _settings!!
            _settings = appIcon(name = "AppIcons.Settings") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,8f)
                    lineTo(16f,12f)
                    lineTo(12f,16f)
                    lineTo(8f,12f)
                    lineTo(12f,8f)
                    moveTo(12f,2f)
                    verticalLineTo(5f)
                    moveTo(12f,19f)
                    verticalLineTo(22f)
                    moveTo(4.9f,4.9f)
                    lineTo(7f,7f)
                    moveTo(17f,17f)
                    lineTo(19.1f,19.1f)
                    moveTo(2f,12f)
                    horizontalLineTo(5f)
                    moveTo(19f,12f)
                    horizontalLineTo(22f)
                }
            }
            return _settings!!
        }

    val Share: ImageVector
        get() {
            if (_share != null) return _share!!
            _share = appIcon(name = "AppIcons.Share") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,12f)
                    lineTo(18f,5f)
                    moveTo(6f,12f)
                    lineTo(18f,19f)
                    moveTo(6f,12f)
                    lineTo(6.01f,12f)
                    moveTo(18f,5f)
                    lineTo(18.01f,5f)
                    moveTo(18f,19f)
                    lineTo(18.01f,19f)
                }
            }
            return _share!!
        }

    val Shuffle: ImageVector
        get() {
            if (_shuffle != null) return _shuffle!!
            _shuffle = appIcon(name = "AppIcons.Shuffle") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,7f)
                    horizontalLineTo(8f)
                    lineTo(16f,17f)
                    horizontalLineTo(20f)
                    moveTo(16f,13f)
                    lineTo(20f,17f)
                    lineTo(16f,21f)
                    moveTo(4f,17f)
                    horizontalLineTo(8f)
                    lineTo(10f,15f)
                    moveTo(16f,7f)
                    horizontalLineTo(20f)
                    moveTo(16f,3f)
                    lineTo(20f,7f)
                    lineTo(16f,11f)
                }
            }
            return _shuffle!!
        }

    val SkipNext: ImageVector
        get() {
            if (_skipNext != null) return _skipNext!!
            _skipNext = appIcon(name = "AppIcons.SkipNext") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(6f,5f)
                    lineTo(15f,12f)
                    lineTo(6f,19f)
                    lineTo(6f,5f)
                    moveTo(18f,5f)
                    verticalLineTo(19f)
                }
            }
            return _skipNext!!
        }

    val SkipPrevious: ImageVector
        get() {
            if (_skipPrevious != null) return _skipPrevious!!
            _skipPrevious = appIcon(name = "AppIcons.SkipPrevious") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(18f,5f)
                    lineTo(9f,12f)
                    lineTo(18f,19f)
                    lineTo(18f,5f)
                    moveTo(6f,5f)
                    verticalLineTo(19f)
                }
            }
            return _skipPrevious!!
        }

    val Star: ImageVector
        get() {
            if (_star != null) return _star!!
            _star = appIcon(name = "AppIcons.Star") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,4f)
                    lineTo(14.5f,9f)
                    lineTo(20f,9.8f)
                    lineTo(16f,13.8f)
                    lineTo(17f,19f)
                    lineTo(12f,16.4f)
                    lineTo(7f,19f)
                    lineTo(8f,13.8f)
                    lineTo(4f,9.8f)
                    lineTo(9.5f,9f)
                    lineTo(12f,4f)
                }
            }
            return _star!!
        }

    val TextFields: ImageVector
        get() {
            if (_textFields != null) return _textFields!!
            _textFields = appIcon(name = "AppIcons.TextFields") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(5f,6f)
                    horizontalLineTo(19f)
                    moveTo(12f,6f)
                    verticalLineTo(19f)
                    moveTo(9f,19f)
                    horizontalLineTo(15f)
                }
            }
            return _textFields!!
        }

    val Undo: ImageVector
        get() {
            if (_undo != null) return _undo!!
            _undo = appIcon(name = "AppIcons.Undo") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(10f,7f)
                    lineTo(4f,12f)
                    lineTo(10f,17f)
                    moveTo(5f,12f)
                    horizontalLineTo(16f)
                    lineTo(20f,16f)
                }
            }
            return _undo!!
        }

    val Videocam: ImageVector
        get() {
            if (_videocam != null) return _videocam!!
            _videocam = appIcon(name = "AppIcons.Videocam") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,7f)
                    horizontalLineTo(15f)
                    verticalLineTo(17f)
                    horizontalLineTo(4f)
                    verticalLineTo(7f)
                    moveTo(15f,10f)
                    lineTo(21f,7f)
                    verticalLineTo(17f)
                    lineTo(15f,14f)
                }
            }
            return _videocam!!
        }

    val VpnKey: ImageVector
        get() {
            if (_vpnKey != null) return _vpnKey!!
            _vpnKey = appIcon(name = "AppIcons.VpnKey") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(4f,12f)
                    lineTo(10f,6f)
                    lineTo(16f,12f)
                    lineTo(10f,18f)
                    lineTo(4f,12f)
                    moveTo(16f,12f)
                    horizontalLineTo(22f)
                    moveTo(19f,12f)
                    verticalLineTo(15f)
                }
            }
            return _vpnKey!!
        }

    val WarningAmber: ImageVector
        get() {
            if (_warningAmber != null) return _warningAmber!!
            _warningAmber = appIcon(name = "AppIcons.WarningAmber") {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    moveTo(12f,4f)
                    lineTo(21f,20f)
                    horizontalLineTo(3f)
                    lineTo(12f,4f)
                    moveTo(12f,9f)
                    verticalLineTo(14f)
                    moveTo(12f,17f)
                    lineTo(12.01f,17f)
                }
            }
            return _warningAmber!!
        }

    private var _accessTime: ImageVector? = null
    private var _add: ImageVector? = null
    private var _alternateEmail: ImageVector? = null
    private var _arrowBackIosNew: ImageVector? = null
    private var _attachFile: ImageVector? = null
    private var _barChart: ImageVector? = null
    private var _block: ImageVector? = null
    private var _brokenImage: ImageVector? = null
    private var _brush: ImageVector? = null
    private var _bugReport: ImageVector? = null
    private var _cameraAlt: ImageVector? = null
    private var _check: ImageVector? = null
    private var _checkCircle: ImageVector? = null
    private var _clear: ImageVector? = null
    private var _close: ImageVector? = null
    private var _contentCopy: ImageVector? = null
    private var _create: ImageVector? = null
    private var _crop: ImageVector? = null
    private var _delete: ImageVector? = null
    private var _deleteForever: ImageVector? = null
    private var _done: ImageVector? = null
    private var _doneAll: ImageVector? = null
    private var _download: ImageVector? = null
    private var _edit: ImageVector? = null
    private var _errorOutline: ImageVector? = null
    private var _exitToApp: ImageVector? = null
    private var _favorite: ImageVector? = null
    private var _favoriteBorder: ImageVector? = null
    private var _formatColorFill: ImageVector? = null
    private var _formatColorText: ImageVector? = null
    private var _forum: ImageVector? = null
    private var _group: ImageVector? = null
    private var _image: ImageVector? = null
    private var _info: ImageVector? = null
    private var _keyboardArrowRight: ImageVector? = null
    private var _language: ImageVector? = null
    private var _libraryMusic: ImageVector? = null
    private var _link: ImageVector? = null
    private var _mic: ImageVector? = null
    private var _moreVert: ImageVector? = null
    private var _movie: ImageVector? = null
    private var _musicNote: ImageVector? = null
    private var _pause: ImageVector? = null
    private var _person: ImageVector? = null
    private var _playArrow: ImageVector? = null
    private var _redo: ImageVector? = null
    private var _refresh: ImageVector? = null
    private var _repeat: ImageVector? = null
    private var _reply: ImageVector? = null
    private var _search: ImageVector? = null
    private var _send: ImageVector? = null
    private var _settings: ImageVector? = null
    private var _share: ImageVector? = null
    private var _shuffle: ImageVector? = null
    private var _skipNext: ImageVector? = null
    private var _skipPrevious: ImageVector? = null
    private var _star: ImageVector? = null
    private var _textFields: ImageVector? = null
    private var _undo: ImageVector? = null
    private var _videocam: ImageVector? = null
    private var _vpnKey: ImageVector? = null
    private var _warningAmber: ImageVector? = null
}

private fun appIcon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()
}
