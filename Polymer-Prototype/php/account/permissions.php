<?php
	function isAdmin($conn, $username) {
		$php = "SELECT * FROM admin WHERE 
				a_username = \"" .$username. "\";";
		$result = $conn->query($sql);
		return ($result->num_rows > 0);
	}

	function isOwner($conn, $username, $channelname, $channelowner) {
		return (strcasecmp($username, $channelowner) == 0)
	}

	function isModerator($conn, $username, $channelname, $channelowner) {
		$php = "SELECT * FROM channelmods WHERE 
			cm_username = \"" .$username. "\" AND 
			cm_chname = \"" .$channelname. "\" AND 
			cm_chowner = \"" .$channelowner. "\";";
		$result = $conn->query($sql);
		return ($result->num_rows > 0);
	}

	function isViewer($conn, $username, $channelname, $channelowner) {
		$php = "SELECT * FROM private_view_channels WHERE 
			prv_username = \"" .$username. "\" AND 
			prv_chname = \"" .$channelname. "\" AND 
			prv_chowner = \"" .$channelowner. "\";";
		$result = $conn->query($sql);
		return ($result->num_rows > 0);
	}

	function isBanned($conn, $username, $channelname, $channelowner) {
		$php = "SELECT * FROM private_view_channels WHERE 
			cb_username = \"" .$username. "\" AND 
			cb_chname = \"" .$channelname. "\" AND 
			cb_chowner = \"" .$channelowner. "\";";
		$result = $conn->query($sql);
		return ($result->num_rows > 0);
	}
?>