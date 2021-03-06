""" alpha-beta minimax agent """
import random
import numpy as np
from os import path
from feature import diff
from copy import deepcopy
from sklearn.externals import joblib
from scipy.signal import convolve2d as conv2d


INFINITY = 1000
DANGER = 100
DISCOUNT = 0.99
FILTER = np.array([
    [1, 0, 1, 0, 1],
    [0, 1, 1, 1, 0],
    [1, 1, 1, 1, 1],
    [0, 1, 1, 1, 0],
    [1, 0, 1, 0, 1],
]).astype(np.int8)


class BlackOps:
    @staticmethod
    def get_op_live_four(f):
        return f["-xxxx-"]
    @staticmethod
    def get_op_four(f):
        return f["four-x"] + f["-xxxxo"]
    @staticmethod
    def get_op_three(f):
        return f["-x-xx-"] + f["-xxx-"]
    @staticmethod
    def get_op_potential(f):
        return f["-xx-"] + f["-x-x-"] + f["-x-xxo"] + f["-xxxo"]
    @staticmethod
    def get_my_live_four(f):
        return f["-oooo-"]
    @staticmethod
    def get_my_four(f):
        return f["four-o"] + f["-oooox"]
    @staticmethod
    def get_my_three(f):
        return f["-o-oo-"] + f["-ooo-"]
    @staticmethod
    def get_my_potential(f):
        return f["-oo-"] + f["-o-o-"] + f["-o-oox"] + f["-ooox"]
    

class WhiteOps:
    @staticmethod
    def get_my_live_four(f):
        return f["-xxxx-"]
    @staticmethod
    def get_my_four(f):
        return f["four-x"] + f["-xxxxo"]
    @staticmethod
    def get_my_three(f):
        return f["-x-xx-"] + f["-xxx-"]
    @staticmethod
    def get_my_potential(f):
        return f["-xx-"] + f["-x-x-"] + f["-x-xxo"] + f["-xxxo"]
    @staticmethod
    def get_op_live_four(f):
        return f["-oooo-"]
    @staticmethod
    def get_op_four(f):
        return f["four-o"] + f["-oooox"]
    @staticmethod
    def get_op_three(f):
        return f["-o-oo-"] + f["-ooo-"]
    @staticmethod
    def get_op_potential(f):
        return f["-oo-"] + f["-o-o-"] + f["-o-oox"] + f["-ooox"]


fundamental = {
    "-xxo": 0, "-x-xo": 1, "-oox": 2, "-o-ox": 3,
    "-x-x-": 4, "-xx-": 5, "-o-o-": 6, "-oo-": 7,
    "-x-xxo": 8, "-xxxo": 9, "-o-oox": 10, "-ooox": 11,
    "-x-xx-": 12, "-xxx-": 13, "-o-oo-": 14, "-ooo-": 15,
    "-xxxx-": 16, "-xxxxo": 17, "-oooo-": 18, "-oooox": 19,
    "win-o": 20, "win-x": 21, "four-o": 22, "four-x": 23,
    "o-o-o": 24, "x-x-x": 25, "violate": 26
}
def translate(features, player):
    feature = np.zeros(shape=29, dtype=int)
    if player == 1:
        feature[27] = 1
    else:
        feature[28] = 1
    for k, v in features.items():
        if v > 0:
            feature[fundamental[k]] = v
    return feature


class MinimaxAgent:
    def __init__(self, max_depth=6, max_width=8):
        self.max_depth = max_depth
        self.max_width = max_width
        self.ops = [None, BlackOps(), WhiteOps()]
        self.clf = joblib.load(path.join(path.dirname(__file__), "model", "value", "svm", "svm-avg-rbf-3.pkl"))

    def random_action(self, state):
        adjacent = conv2d(np.abs(state.board), FILTER, mode="same")
        prob = np.logical_and(state.board == 0, adjacent > 0).astype(float)
        for x in range(15):
            for y in range(15):
                if prob[x, y] > 0:
                    new, old = diff(state, x, y)
                    if ("violate" in new or new["-o-oo-"] + new["-ooo-"] >= 2 or
                        new["four-o"] + new["-oooo-"] + new["-oooox"] >= 2):
                        prob[x, y] = 0
        prob = (prob / prob.sum()).reshape(225)
        return np.unravel_index(np.random.choice(225, p=prob), dims=(15, 15))

    def _score(self, features, new, old, player):
        features.add(new)
        features.sub(old)
        feature = translate(features, player)
        return self.clf.predict(feature.reshape(1, 29))[0]

    def _policy(self, state):
        if len(state.history) == 0:
            return [(7, 7)]
        elif len(state.history) == 1:
            return [(6, 7), (6, 6)]
        adjacent = conv2d(np.abs(state.board), FILTER, mode="same")
        actions = []
        ops = self.ops[state.player]
        in_lose_danger = ops.get_op_live_four(state.features) > 0
        in_four_danger = ops.get_op_four(state.features) > 0
        in_three_danger = ops.get_op_three(state.features) > 0
        for x in range(15):
            for y in range(15):
                if adjacent[x, y] > 0 and state.board[x, y] == 0:
                    new, old = diff(state, x, y)
                    if state.player == -1 or ("violate" not in new and
                        new["-o-oo-"] + new["-ooo-"] < 2 and
                        new["four-o"] + new["-oooo-"] + new["-oooox"] < 2):
                        if new["win-o"] > 0 or new["win-x"] > 0:
                            return [(x, y)]
                        elif not in_lose_danger:
                            if in_four_danger:
                                if ops.get_op_four(state.features) == ops.get_op_four(old):
                                    actions.append((x, y, 0, 0))
                            elif ops.get_my_live_four(new) > 0:
                                return [(x, y)]
                            elif in_three_danger:
                                if ops.get_my_four(new) > 0:
                                    actions.append((x, y, 0, 0))
                                elif ops.get_op_three(state.features) == ops.get_op_three(old):
                                    actions.append((x, y, 0, 0))
                            elif len(new) + len(old) > 0:
                                state.player = -state.player
                                _new, _old = diff(state, x, y)
                                _ops = self.ops[state.player]
                                state.player = -state.player
                                actions.append((x, y,
                                    self._score(deepcopy(state.features), new, old, -state.player),
                                    self._score(deepcopy(state.features), _new, _old, state.player)))
        if len(actions) == 0:
            return actions
        random.shuffle(actions)
        width = self.max_width // 2
        return list(set(list(map(lambda t: (t[0], t[1]), sorted(actions, key=lambda t: t[2], reverse=True)))[:width] +
                list(map(lambda t: (t[0], t[1]), sorted(actions, key=lambda t: t[3], reverse=True)))[:width]))

    def _value(self, state):
        ops = self.ops[state.player]
        if ops.get_my_live_four(state.features) > 0 or ops.get_my_four(state.features) > 0:
            return state.player * INFINITY
        if ops.get_op_live_four(state.features) > 0:
            return -state.player * INFINITY
        return self.clf.predict(translate(state.features, state.player).reshape(1, 29))[0]

    def _max_value(self, state, alpha, beta, depth, hist, store):
        if state.end:
            return state.player * INFINITY
        if depth == self.max_depth:
            return self._value(state)
        frozen = frozenset(hist)
        if frozen in store:
            return store[frozen]
        actions = self._policy(state)
        if len(actions) == 0:
            return -INFINITY
        who = state.player
        max_value = -INFINITY
        for x, y in actions:
            hist.add(who * np.ravel_multi_index((x, y), dims=(15, 15)))
            state.move(x, y)
            max_value = max(max_value,
                DISCOUNT * self._min_value(state, alpha, beta, depth + 1, hist, store))
            if max_value > beta:
                hist.remove(who * np.ravel_multi_index((x, y), dims=(15, 15)))
                state.rewind()
                store[frozenset(hist)] = max_value
                return max_value
            alpha = max(alpha, max_value)
            hist.remove(who * np.ravel_multi_index((x, y), dims=(15, 15)))
            state.rewind()
        store[frozenset(hist)] = max_value
        return max_value

    def _min_value(self, state, alpha, beta, depth, hist, store):
        if state.end:
            return state.player * INFINITY
        if depth == self.max_depth:
            return self._value(state)
        frozen = frozenset(hist)
        if frozen in store:
            return store[frozen]
        actions = self._policy(state)
        if len(actions) == 0:
            return INFINITY
        who = state.player
        min_value = INFINITY
        for x, y in actions:
            hist.add(who * np.ravel_multi_index((x, y), dims=(15, 15)))
            state.move(x, y)
            min_value = min(min_value,
                DISCOUNT * self._max_value(state, alpha, beta, depth + 1, hist, store))
            if min_value < alpha:
                hist.remove(who * np.ravel_multi_index((x, y), dims=(15, 15)))
                state.rewind()
                store[frozenset(hist)] = min_value
                return min_value
            beta = min(beta, min_value)
            hist.remove(who * np.ravel_multi_index((x, y), dims=(15, 15)))
            state.rewind()
        store[frozenset(hist)] = min_value
        return min_value

    def get_action(self, state):
        dist = self.get_dist(state)
        prob = np.array(list(map(lambda t: t[2], dist)))
        choice = np.random.choice(prob.shape[0], p=prob)
        x, y, _ = dist[choice]
        return x, y

    def get_dist(self, state):
        score = self.get_score(state)
        prob = np.array(list(map(lambda t: state.player * t[2], score)))
        prob = np.exp(prob - prob.max())
        prob = prob / prob.sum()
        return list(map(lambda i: (score[i][0], score[i][1], prob[i]), range(len(score))))

    def get_score(self, state):
        actions = self._policy(state)
        if len(actions) == 0:
            x, y = self.random_action(state)
            return [(x, y, 0)]
        if len(actions) == 1:
            x, y = actions[0]
            return [(x, y, 0)]
        who = state.player
        store = dict()
        def evaluate(action):
            x, y = action
            hist = { who * np.ravel_multi_index((x, y), dims=(15, 15)) }
            state.move(x, y)
            if who == 1:
                val = self._min_value(state, -INFINITY, INFINITY, 1, hist, store)
            else:
                val = self._max_value(state, -INFINITY, INFINITY, 1, hist, store)
            state.rewind()
            return x, y, val
        return list(map(evaluate, actions))
